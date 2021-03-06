package ai.evolv.android_sdk;

import android.util.Log;
import android.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ai.evolv.android_sdk.evolvinterface.EvolvCallBack;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.helper.UtilityHelper;

import static ai.evolv.android_sdk.EvolvConfig.DEFAULT_VERSION;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_CHANGED;

class EvolvStoreImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvStoreImpl.class);

    public static String GENOME_REQUEST_SENT = "genome.request.sent";
    public static String CONFIG_REQUEST_SENT = "config.request.sent";
    public static String GENOME_REQUEST_RECEIVED = "genome.request.received";
    public static String CONFIG_REQUEST_RECEIVED = "config.request.received";
    public static String REQUEST_FAILED = "request.failed";
    public static String GENOME_UPDATED = "genome.updated";
    public static String CONFIG_UPDATED = "config.updated";
    public static String EFFECTIVE_GENOME_UPDATED = "effective.genome.updated";
    public static String STORE_DESTROYED = "store.destroyed";
    public static String GENOME_STRING = "genome";
    public static String AUDIENCE_QUERY_STRING = "audience_query";

    public static String EMPTY_STRING = "";


    private EvolvConfig evolvConfig;
    ListenableFuture<JsonObject> futureConfiguration;
    ListenableFuture<JsonArray> futureAllocations;
    private boolean initialized = false;
    private EvolvContext evolvContext;
    private EvolvParticipant participant;
    Allocator allocator;
    JsonObject config = new JsonObject();
    private UtilityHelper helper;
    private boolean configFailed = false;
    private JsonObject clientContext;
    private JsonArray allocations;
    private boolean genomeFailed = false;
    private KeyStates configKeyStates = new KeyStates();
    private KeyStates genomeKeyStates = new KeyStates();
    private WaitForIt waitForIt;
    private EvolvPredicatesImpl evolvPredicates;
    private int version;
    private boolean reevaluatingContext = false;
    private JsonObject genomes = new JsonObject();
    private JsonObject effectiveGenome = new JsonObject();
    JsonArray activeEids = new JsonArray();
    private JsonObject activeKeys = new JsonObject();
    private JsonObject activeVariants = new JsonObject();
    private CopyOnWriteArrayList<String> expLoadedList = new CopyOnWriteArrayList<>();
    private CountDownLatch latch = new CountDownLatch(1);
    private Map<EvolvCallBack, Pair<String, EvolvType>> subscriptions = new LinkedHashMap<>();
    private ExecutorService executor;
    private Future<?> future = null;

    @FunctionalInterface
    interface Filter<T> {
        boolean apply(T key);
    }

    EvolvInvocation invocation = payload -> {
        reevaluateContext();
    };

    static class KeyStates {

        Set<String> needed = new HashSet<String>();
        Set requested = new HashSet();
        JsonObject experiments = new JsonObject();
    }

    public EvolvStoreImpl(EvolvConfig evolvConfig,
                          EvolvParticipant participant,
                          WaitForIt waitForIt
    ) {
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.waitForIt = waitForIt;
        evolvPredicates = new EvolvPredicatesImpl();
        allocator = new Allocator(evolvConfig, participant);
        helper = new UtilityHelper();
        setVersion();
    }

    public Allocator getAllocator() {
        return allocator;
    }

    private void setVersion() {
        version = evolvConfig.getVersion() != 0 ? evolvConfig.getVersion() : DEFAULT_VERSION;
    }

    void initialize(EvolvContext context) {
        if (initialized) {
            try {
                throw new EvolvKeyError("Evolv: The store has already been initialized.");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        this.evolvContext = context;
        initialized = true;
        pull();

        waitForIt.waitFor(evolvContext, CONTEXT_CHANGED, invocation);
    }

    void pull() {
        if (configKeyStates.needed.size() != 0 || version == DEFAULT_VERSION) {

            JsonArray requestedKeys = new JsonArray();

            for (String requestedKey : configKeyStates.needed) {
                requestedKeys.add(requestedKey);
            }
            configKeyStates.needed.clear();

            // fetch configuration asynchronously
            fetchConfiguration(requestedKeys);
            waitForIt.emit(evolvContext, CONFIG_REQUEST_SENT, requestedKeys);
        }

        if (genomeKeyStates.needed.size() != 0 || version == DEFAULT_VERSION) {

            JsonArray requestedKeys = new JsonArray();

            for (String requestedKey : genomeKeyStates.needed) {
                requestedKeys.add(requestedKey);
            }
            genomeKeyStates.needed.clear();

            // fetch and reconcile allocations asynchronously
            fetchAllocations(requestedKeys);
            waitForIt.emit(evolvContext, GENOME_REQUEST_SENT, requestedKeys);

        }
    }

    void pull(boolean immediate) {
        if (configKeyStates.needed.size() != 0 || version == DEFAULT_VERSION) {

            JsonArray requestedKeys = new JsonArray();

            for (String requestedKey : configKeyStates.needed) {
                requestedKeys.add(requestedKey);
            }

            configKeyStates.needed.clear();

            // fetch configuration asynchronously
            fetchConfiguration(requestedKeys);
            waitForIt.emit(evolvContext, CONFIG_REQUEST_SENT, requestedKeys);
        }

        if (genomeKeyStates.needed.size() != 0 || version == DEFAULT_VERSION) {

            JsonArray requestedKeys = new JsonArray();

            for (String requestedKey : genomeKeyStates.needed) {
                requestedKeys.add(requestedKey);
            }

            genomeKeyStates.needed.clear();

            // fetch and reconcile allocations asynchronously
            fetchAllocations(requestedKeys);
            waitForIt.emit(evolvContext, GENOME_REQUEST_SENT, requestedKeys);

        }
    }

    private void fetchAllocations(JsonArray requestedKeys) {
        ListenableFuture<String> responseFutureAllocations = allocator.fetchAllocations();
        SettableFuture<JsonArray> setFutureAllocations = SettableFuture.create();

        responseFutureAllocations.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonArray allocations = JsonParser.parseString(responseFutureAllocations.get()).getAsJsonArray();
                    setFutureAllocations.set(allocations);
                    futureAllocations = setFutureAllocations;

                    update(false, requestedKeys, allocations);

                } catch (Exception e) {
                    Log.d("pull_evolv", "There was a failure while retrieving the allocations.", e);

                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void fetchConfiguration(JsonArray requestedKeys) {
        ListenableFuture<String> responseFutureConfiguration = allocator.fetchConfiguration();
        SettableFuture<JsonObject> setFutureConfiguration = SettableFuture.create();

        responseFutureConfiguration.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonObject configuration = JsonParser.parseString(responseFutureConfiguration.get()).getAsJsonObject();
                    setFutureConfiguration.set(configuration);
                    futureConfiguration = setFutureConfiguration;

                    latch.await();

                    update(true, requestedKeys, configuration);

                } catch (Exception e) {
                    LOGGER.error("There was a failure while retrieving the configuration.", e);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void update(boolean configRequest, JsonArray requestedKeys, JsonElement value) {

        KeyStates keyStates = configRequest ? configKeyStates : genomeKeyStates;
        keyStates.requested.clear();

        if (configRequest) {
            waitForIt.emit(evolvContext, CONFIG_REQUEST_RECEIVED, requestedKeys);
            if (value instanceof JsonObject) {
                updateConfig(value.getAsJsonObject());
                reevaluateContext();
            }
        } else {
            waitForIt.emit(evolvContext, GENOME_REQUEST_RECEIVED, requestedKeys);
            if (value instanceof JsonArray) {
                updateGenome((JsonArray) value);
            }
        }
    }

    void reevaluateContext() {

        if (config.isJsonNull() || config.size() == 0) {
            return;
        }
        if (reevaluatingContext) {
            return;
        }
        reevaluatingContext = true;

        setActiveAndEntryKeyStates(version, evolvContext, config, allocations, configKeyStates);
        JsonObject result = generateEffectiveGenome(configKeyStates.experiments, genomes);

        if (result.size() != 0) {
            effectiveGenome = result.get("effectiveGenome").getAsJsonObject();
            activeEids.add(result.get("activeEids"));
        } else {
            clearActiveGenome();
            clearActiveEids();
        }

        clearActiveKeysImpl();
        clearActiveVariantsImpl();

        for (Map.Entry<String, JsonElement> expKeyStates : configKeyStates.experiments.entrySet()) {
            JsonObject active = expKeyStates.getValue().getAsJsonObject().get("active").getAsJsonObject();

            for (Map.Entry<String, JsonElement> activeKey : active.getAsJsonObject().entrySet()) {
                activeKeys.addProperty(activeKey.getKey(), activeKey.getValue().getAsString());

                if (effectiveGenome != null && effectiveGenome.size() != 0) {
                    if (effectiveGenome.has("activeGenome_" + expKeyStates.getKey())) {
                        JsonObject value = effectiveGenome.get("activeGenome_" + expKeyStates.getKey()).getAsJsonObject();
                        JsonElement pruned = helper.prune(value, active);

                        for (String key : pruned.getAsJsonObject().keySet()) {
                            activeVariants.addProperty("activeVariants_" + key, key.concat(":" + pruned.getAsJsonObject().get(key).hashCode()));
                        }
                    }
                }
            }
        }

        JsonObject newActiveKeys = activeKeys.deepCopy();
        JsonObject newActiveVariants = activeVariants.deepCopy();

        evolvContext.set("keys.active", newActiveKeys, false);
        evolvContext.set("variants.active", newActiveVariants, false);

        waitForIt.emit(evolvContext, EFFECTIVE_GENOME_UPDATED, effectiveGenome);


        for (Map.Entry<EvolvCallBack, Pair<String, EvolvType>> entry : subscriptions.entrySet()) {
            performAction(entry.getValue().second, entry.getValue().first, entry.getKey());
        }

        reevaluatingContext = false;
    }

    private void clearActiveKeysImpl(String prefix) {
        Map<String, String> mapKeys = new HashMap();
        for (String s : activeKeys.keySet()) {
            mapKeys.put(s, activeKeys.get(s).getAsString());
        }

        for (Map.Entry<String, String> key : mapKeys.entrySet()) {
            if (key.getValue().startsWith(prefix)) {
                activeKeys.remove(key.getKey());
            }
        }
    }

    private void clearActiveKeysImpl() {
        List<String> keys = new ArrayList<>();
        for (String s : activeKeys.keySet()) {
            keys.add(s);
        }

        for (String key : keys) {
            activeKeys.remove(key);
        }
    }

    private void clearActiveGenome() {
        List<String> keys = new ArrayList<>();
        for (String s : effectiveGenome.keySet()) {
            keys.add(s);
        }

        for (String key : keys) {
            effectiveGenome.remove(key);
        }
    }

    private void clearActiveEids() {
        List<JsonElement> keys = new ArrayList<>();
        for (JsonElement s : activeEids) {
            keys.add(s);
        }

        for (JsonElement key : keys) {
            activeEids.remove(key);
        }
    }

    private void clearActiveVariantsImpl() {
        List<String> keys = new ArrayList<>();
        for (String s : activeVariants.keySet()) {
            keys.add(s);
        }

        for (String key : keys) {
            activeVariants.remove(key);
        }
    }

    private JsonObject generateEffectiveGenome(JsonObject expsKeyStates, JsonObject genomes) {

        JsonObject effectiveObject = new JsonObject();
        JsonObject effectiveGenome = new JsonObject();
        JsonArray activeEids = new JsonArray();

        for (Map.Entry<String, JsonElement> entryExp : expsKeyStates.entrySet()) {

            String eid = entryExp.getKey();
            JsonObject expKeyStates = entryExp.getValue().getAsJsonObject();

            JsonObject active = new JsonObject();
            if (expKeyStates.has("active")) {
                active = expKeyStates.get("active").getAsJsonObject();
            }

            if (genomes.has(eid) && active.getAsJsonObject().size() != 0) {

                JsonObject cloneObject = genomes.get(eid).deepCopy().getAsJsonObject();

                JsonObject activeGenome = helper.filter(cloneObject, active);

                if (activeGenome.keySet().size() != 0) {
                    activeEids.add(eid);
                    effectiveGenome.add("activeGenome_" + eid, activeGenome.deepCopy());

                }
            }
        }
        if (activeEids.size() != 0 || effectiveGenome.size() != 0) {
            effectiveObject.add("activeEids", activeEids);
            effectiveObject.add("effectiveGenome", effectiveGenome);
        }

        return effectiveObject;
    }

    void setActiveAndEntryKeyStates(int version,
                                    EvolvContext evolvContext,
                                    JsonObject config,
                                    JsonArray allocations,
                                    KeyStates configKeyStates) {

        JsonArray results = evaluatePredicates(version, evolvContext, config);

        for (JsonElement result : results) {
            String eid = "";
            JsonObject expResults = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : result.getAsJsonObject().entrySet()) {
                eid = getEID(entry.getKey());
                break;
            }

            expResults.add("disabled", result.getAsJsonObject().get(eid + "_disabled"));
            expResults.add("entry", result.getAsJsonObject().get(eid + "_entry"));

            JsonObject expConfigKeyStates = configKeyStates.experiments.get(eid).getAsJsonObject();

            JsonObject activeKeyStates = new JsonObject();
            JsonObject entryKeyStates = new JsonObject();

            if (expConfigKeyStates.isJsonNull()) {
                return;
            }

            JsonArray expConfigLoaded = expConfigKeyStates.get("loaded").getAsJsonObject().get("loaded_keys").getAsJsonArray();

            JsonObject loadedKeys = new JsonObject();
            for (JsonElement key : expConfigLoaded) {
                loadedKeys.addProperty(key.getAsString(), key.getAsString());
            }

            JsonObject newExpKeyStates = getActiveAndEntryExperimentKeyStates(expResults, loadedKeys);
            //active
            if (newExpKeyStates.get("active").getAsJsonObject().size() != 0) {
                for (String key : newExpKeyStates.get("active").getAsJsonObject().keySet()) {
                    activeKeyStates.addProperty("active_" + key, key);
                }
            }

            //entry
            if (newExpKeyStates.get("entry").getAsJsonObject().size() != 0) {
                for (String key : newExpKeyStates.get("entry").getAsJsonObject().keySet()) {
                    entryKeyStates.addProperty("entry_" + key, key);
                }
            }

            expConfigKeyStates.add("active", activeKeyStates);
            expConfigKeyStates.add("entry", entryKeyStates);
        }
    }

    private String getEID(String key) {
        return key.substring(0, key.indexOf("_"));
    }

    private JsonObject getActiveAndEntryExperimentKeyStates(JsonObject results, JsonObject keyStatesLoaded) {

        JsonObject expKeyStates = new JsonObject();

        JsonObject active = new JsonObject();
        JsonObject entry = new JsonObject();

        expKeyStates.add("active", active);
        expKeyStates.add("entry", entry);

        for (String key : keyStatesLoaded.keySet()) {
            boolean activeKey = true;
            boolean entryPointKey = false;

            for (Map.Entry<String, JsonElement> rejectedEntry : results.get("disabled").getAsJsonObject().entrySet()) {
                if (key.startsWith(rejectedEntry.getValue().getAsString())) {
                    activeKey = false;
                    break;
                }
            }

            if (activeKey) {
                active.addProperty(key, key);
                for (Map.Entry<String, JsonElement> entryPoint : results.get("entry").getAsJsonObject().entrySet()) {
                    if (key.startsWith(entryPoint.getValue().getAsString())) {
                        entryPointKey = true;
                    }

                    if (entryPointKey) {
                        entry.addProperty(key, key);
                    }
                }
            }
        }

        return expKeyStates;
    }

    private void updateGenome(JsonArray value) {

        JsonArray allocs = new JsonArray();
        JsonObject exclusions = new JsonObject();

        allocations = value;
        genomeFailed = false;

        for (JsonElement jsonElement : value) {
            JsonObject alloc = jsonElement.getAsJsonObject();
            JsonObject clean = alloc.deepCopy();

            clean.remove(GENOME_STRING);
            clean.remove(AUDIENCE_QUERY_STRING);

            allocs.add(clean);

            if (clean.has("excluded")) {
                if (clean.get("excluded").getAsBoolean()) {
                    exclusions.addProperty("exclusions", clean.get("eid").getAsString());
                    return;
                }
            }

            genomes.add(clean.get("eid").getAsString(), alloc.get("genome"));
            JsonObject expLoaded = new JsonObject();
            JsonObject expMap = new JsonObject();

            expMap.add("loaded", expLoaded);
            genomeKeyStates.experiments.add(clean.get("eid").getAsString(), expMap);

            expLoadedList.clear();
            flattenKeys(alloc.get("genome"));

            JsonArray jsonArray = JsonParser.parseString(expLoadedList.toString()).getAsJsonArray();

            expLoaded.add("loaded_keys", jsonArray);

        }

        evolvContext.set("experiments.allocations", allocs, false);
        evolvContext.set("experiments.exclusions", exclusions, false);

        latch.countDown();
    }

    private void updateConfig(JsonObject value) {
        config = value;
        configFailed = false;

        if (config.has("_client")) {
            clientContext = config.getAsJsonObject("_client");
        }

        if (value.has("_experiments")) {
            Iterator<JsonElement> iterator = value.get("_experiments").getAsJsonArray().iterator();

            while (iterator.hasNext()) {
                JsonObject exp = iterator.next().getAsJsonObject();
                setConfigLoadedKeys(configKeyStates, exp);
            }
        } else {
            LOGGER.error("Failed to find \"_experiments\" field from config");
        }
    }

    private void setConfigLoadedKeys(KeyStates keyStates, JsonObject exp) {
        JsonObject clean = exp.deepCopy();
        if (clean.has("id")) clean.remove("id");

        JsonObject expLoaded = new JsonObject();
        JsonObject expMap = new JsonObject();

        expMap.add("loaded", expLoaded);

        keyStates.experiments.add(exp.get("id").getAsString(), expMap);

        expLoadedList.clear();
        flattenKeys(clean);

        endsWithFilter();
        JsonArray jsonArray = JsonParser.parseString(expLoadedList.toString()).getAsJsonArray();

        expLoaded.add("loaded_keys", jsonArray);

    }

    void endsWithFilter() {
        for (int i = expLoadedList.size() - 1; i >= 0; i--) {
            if (endsWithFilter.apply(expLoadedList.get(i))) {
                expLoadedList.remove(i);
            }
        }
    }

    Filter<String> startsWithFilter = key -> !key.startsWith("_")
            || key.equals("_values")
            || key.equals("_initializers");

    Filter<String> endsWithFilter = key -> key.endsWith("_values")
            || key.endsWith("_initializers");

    private void flattenKeys(JsonElement map) {
        recurse(map, "");
    }

    public String recurse(JsonElement current, String parentKey) {
        Set<String> keys = current.getAsJsonObject().keySet();

        Iterator<String> iterator = keys.iterator();
        Set<String> items = new HashSet<>();

        while (iterator.hasNext()) {
            String key = iterator.next();

            if (startsWithFilter.apply(key)) {
                JsonElement element = current.getAsJsonObject().get(key);
                if (element.isJsonObject()) {
                    if (element.getAsJsonObject().size() == 0) continue;
                }
                String newKey = !parentKey.isEmpty() ? (parentKey + '.' + key) : key;
                items.add(newKey);
                expLoadedList.add(newKey);

                if (element.isJsonObject()) {
                    if (element.getAsJsonObject().size() != 0) {
                        element = current.getAsJsonObject().get(key);
                        items.add(key.concat(recurse(element, newKey)));
                    }
                }
            }
        }

        return EMPTY_STRING;
    }

    JsonArray evaluatePredicates(int version, EvolvContext evolvContext, JsonElement config) {
        JsonArray result = new JsonArray();

        if (!config.getAsJsonObject().has("_experiments"))
            if (config.getAsJsonObject().size() == 0 || config.getAsJsonObject().getAsJsonArray("_experiments").size() == 0)
                return result;
        JsonElement evaluableContext = evolvContext.resolve();
        Iterator<JsonElement> iterator = config
                .getAsJsonObject()
                .get("_experiments")
                .getAsJsonArray()
                .iterator();

        while (iterator.hasNext()) {
            JsonObject exp = iterator.next().getAsJsonObject();

            JsonObject evaluableConfig = exp.deepCopy();
            if (evaluableConfig.has("id")) evaluableConfig.remove("id");

            JsonObject expResult = new JsonObject();
            JsonObject disabled = new JsonObject();
            JsonObject entry = new JsonObject();

            expResult.add(exp.get("id").getAsString() + "_disabled", disabled);
            expResult.add(exp.get("id").getAsString() + "_entry", entry);

            evaluateBranch(evaluableContext, evaluableConfig, "", disabled, entry);

            result.add(expResult);
        }

        return result;
    }

    private void evaluateBranch(JsonElement context,
                                JsonElement config,
                                String prefix,
                                JsonObject disabled,
                                JsonObject entry
    ) {

        if (config.isJsonNull() || !config.isJsonObject()) return;


        if (config.getAsJsonObject().has("_predicate")) {
            if (!config.getAsJsonObject().get("_predicate").isJsonNull()) {
                JsonElement result = evolvPredicates.evaluate(context, config.getAsJsonObject().get("_predicate"));

                if (result.getAsJsonObject().has("rejected")) {
                    //here we see if the experiment rejected
                    JsonObject rejected = result.getAsJsonObject().get("rejected").getAsJsonObject();

                    if (rejected.get("rejected").getAsBoolean()) {
                        disabled.addProperty("rejected_" + prefix, prefix);
                        return;
                    }
                }
            }
        }
        if (config.getAsJsonObject().has("_is_entry_point")) {
            if (config.getAsJsonObject().get("_is_entry_point").getAsBoolean()) {
                entry.addProperty("_entry", prefix);
            }
        }

        Set<String> keys = config.getAsJsonObject().keySet();

        for (String key : keys) {
            if (key.startsWith("_")) {
                continue;
            }
            evaluateBranch(context, config.getAsJsonObject().get(key), !prefix.isEmpty() ? prefix + '.' + key : key, disabled, entry);
        }
    }

    public List<String> getExpLoadedList() {
        return expLoadedList;
    }

    JsonArray activeEntryPoints() {
        JsonArray eids = new JsonArray();

        for (Map.Entry<String, JsonElement> entryExperiment : configKeyStates.experiments.entrySet()) {

            JsonObject entryObject = entryExperiment.getValue().getAsJsonObject().get("entry").getAsJsonObject();

            if (entryObject.size() != 0) {
                eids.add(entryExperiment.getKey());
            }
        }
        return eids;
    }

    public void setGenomes(JsonObject genomes) {
        this.genomes = genomes;
    }

    public void setActiveKeys(JsonObject activeKeys) {
        this.activeKeys = activeKeys;
    }

    void subscribe(EvolvType type, String value, EvolvCallBack callBack) {
        Pair<String, EvolvType> pair = new Pair<>(value, type);
        subscriptions.put(callBack, pair);

        performAction(type, value, callBack);
    }

    private void performAction(EvolvType type, String value, EvolvCallBack callBack) {

        executor = executor == null ? evolvConfig.getExecutorService() : executor;

        future = executor.submit(() -> {
            switch (type) {
                case getActiveKeys: {
                    if (value.isEmpty()) {
                        JsonObject activeKeys = getActiveKeys();
                        callBack.invoke(activeKeys);
                    } else {
                        JsonObject activeKeysPrefix = getActiveKeys(value);
                        callBack.invoke(activeKeysPrefix);
                    }
                    break;
                }
                case get: {
                    JsonElement element = getValue(value);
                    JsonElement result = JsonNull.INSTANCE;

                    if (element == null) {
                        result = JsonNull.INSTANCE;
                        callBack.invoke(result);
                        break;
                    }
                    if (element.isJsonPrimitive()) {
                        result = element.getAsJsonPrimitive();
                    } else if (element.isJsonObject()) {
                        result = element.getAsJsonObject();
                    }
                    callBack.invoke(result);
                    break;
                }
                case isActive: {
                    boolean isActive = getValueActive(value);
                    callBack.invoke(isActive);
                    break;
                }
                case activeEntryPoints: {
                    JsonElement activeEntryPoints = activeEntryPoints();
                    callBack.invoke(activeEntryPoints);
                    break;
                }
                default:
            }
        });
    }

    JsonObject getActiveKeys(String prefix) {

        JsonObject result = new JsonObject();

        for (Map.Entry<String, JsonElement> key : activeKeys.entrySet()) {
            if (hasPrefix(key.getValue().getAsString(), prefix)) {
                result.addProperty("prefix_" + key.getKey(), key.getValue().getAsString());
            }
        }
        return result;
    }

    JsonObject getActiveKeys() {
        return activeKeys;
    }

    private boolean hasPrefix(String key, String prefix) {
        return key.startsWith(prefix);
    }

    void preload(ArrayList<String> prefixes) {
        boolean configOnly = false;
        boolean immediate = false;

        preload(prefixes, configOnly, immediate);
    }

    void preload(ArrayList<String> prefixes, boolean configOnly) {
        boolean immediate = false;

        preload(prefixes, configOnly, immediate);
    }

    void preload(ArrayList<String> prefixes, boolean configOnly, boolean immediate) {

        configKeyStates.needed.addAll(prefixes);
        if (!configOnly) {
            genomeKeyStates.needed.addAll(prefixes);
        }

        pull(immediate);
    }

    boolean getValueActive(String key) {
        for (Map.Entry<String, JsonElement> entry : activeKeys.entrySet()) {
            if (entry.getValue().getAsString().equals(key)) {
                return true;
            }
        }
        return false;
    }

    JsonElement getValue(String key) {
        for (Map.Entry<String, JsonElement> genomeEntry : effectiveGenome.entrySet()) {
            JsonElement element = helper.getValueForKey(key, genomeEntry.getValue());
            if (element == null) {
                continue;
            } else {
                return element;
            }
        }
        return JsonNull.INSTANCE;
    }

    void clearActiveKeys(String prefix) {
        clearActiveKeysImpl(prefix);
    }

    void clearActiveKeys() {
        clearActiveKeysImpl();
    }


}
