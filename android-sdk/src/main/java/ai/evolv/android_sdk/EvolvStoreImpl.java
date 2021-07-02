package ai.evolv.android_sdk;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    JsonObject config;
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
    private JsonObject genomes;
    private JsonObject effectiveGenome;
    private JsonObject activeKeys = new JsonObject();
    private JsonObject activeVariants = new JsonObject();
    private List<String> expLoadedList = new ArrayList<>();

    @FunctionalInterface
    interface Filter<T> {
        boolean apply(T key);
    }

    EvolvInvocation invocation = value -> {

        switch (value.toString()) {
            case "reevaluateContext":
                reevaluateContext();
                break;
            default:
                LOGGER.error("Failed to invoke handler of " + value.toString());
                break;
        }
    };

    private class KeyStates {

        Set needed = new HashSet();
        Set requested = new HashSet();
        JsonArray experiments = new JsonArray();
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

            List<Object> requestedKeys = new ArrayList<>(configKeyStates.needed);
            configKeyStates.needed.clear();

            // fetch configuration asynchronously
            fetchConfiguration(requestedKeys);
            waitForIt.emit(evolvContext, CONFIG_REQUEST_SENT, requestedKeys);
        }

        if (genomeKeyStates.needed.size() != 0 || version == DEFAULT_VERSION) {

            List<Object> requestedKeys = new ArrayList<>(genomeKeyStates.needed);
            genomeKeyStates.needed.clear();

            // fetch and reconcile allocations asynchronously
            fetchAllocations();
            waitForIt.emit(evolvContext, GENOME_REQUEST_SENT, requestedKeys);

        }
    }

    private void fetchAllocations() {


        ListenableFuture<String> responseFutureAllocations = allocator.fetchAllocations();
        SettableFuture<JsonArray> setFutureAllocations = SettableFuture.create();

        responseFutureAllocations.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    List<Object> requestedKeys = new ArrayList<>();
                    // TODO: use a non-depreciated method "JsonParser"
                    JsonParser parser = new JsonParser();
                    JsonArray allocations = parser.parse(responseFutureAllocations.get()).getAsJsonArray();
                    Log.d("pull_evolv", "2" + responseFutureAllocations.get());
                    setFutureAllocations.set(allocations);
                    futureAllocations = setFutureAllocations;

                    update(false, requestedKeys, allocations);

                } catch (Exception e) {
                    Log.d("pull_evolv", "There was a failure while retrieving the allocations.", e);

                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void fetchConfiguration(List<Object> requestedKeys) {

        ListenableFuture<String> responseFutureConfiguration = allocator.fetchConfiguration();
        SettableFuture<JsonObject> setFutureConfiguration = SettableFuture.create();
        responseFutureConfiguration.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // TODO: use a non-depreciated method "JsonParser"
                    JsonParser parser = new JsonParser();
                    JsonObject configuration = parser.parse(responseFutureConfiguration.get()).getAsJsonObject();
                    setFutureConfiguration.set(configuration);
                    futureConfiguration = setFutureConfiguration;

                    update(true, requestedKeys, configuration);

                } catch (Exception e) {
                    LOGGER.error("There was a failure while retrieving the configuration.", e);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private void update(boolean configRequest, List<Object> requestedKeys, JsonElement value) {

        KeyStates keyStates = configRequest ? configKeyStates : genomeKeyStates;
        // TODO: 27.05.2021 to figure it out -> requestedKeys.forEach(keyStates.requested.delete.bind(keyStates.requested));
        keyStates.requested.clear();

        if (configRequest) {
            waitForIt.emit(evolvContext, CONFIG_REQUEST_RECEIVED, requestedKeys);
            if (value instanceof JsonObject) {
                updateConfig(value.getAsJsonObject());
            }
        } else {
            waitForIt.emit(evolvContext, GENOME_REQUEST_RECEIVED, requestedKeys);
            if (value instanceof JsonArray) {
                updateGenome((JsonArray) value);
            }
        }

        reevaluateContext();

        // TODO: 01.06.2021 add
    }

    // TODO: 11.06.2021 need a unit test
    private void reevaluateContext() {

        // TODO: 01.06.2021 debug config.isJsonNull()
        if (config.isJsonNull()) {
            return;
        }
        if (reevaluatingContext) {
            return;
        }
        reevaluatingContext = true;

        setActiveAndEntryKeyStates(version, evolvContext, config, allocations, configKeyStates);
        // TODO: 29.06.2021 implement
        Object result = generateEffectiveGenome(configKeyStates.experiments, genomes);

        clearActiveKeys();
        clearActiveVariants();


        for (JsonElement experiment : configKeyStates.experiments) {

            for (Map.Entry<String, JsonElement> elementEntry : experiment.getAsJsonObject().entrySet()) {

                for (JsonElement jsonElement : elementEntry.getValue().getAsJsonArray()) {

                    for (Map.Entry<String, JsonElement> activeKeyEntry : jsonElement.getAsJsonObject().entrySet()) {
                        if(activeKeyEntry.getKey().startsWith("active_")){
                            activeKeys.addProperty(activeKeyEntry.getKey(),activeKeyEntry.getValue().getAsString());
                        }
                    }
                }
            }
            // TODO: 30.06.2021 add pruned
        }

        JsonObject newActiveKeys = activeKeys.deepCopy();
        JsonObject newActiveVariants = activeVariants.deepCopy();

        evolvContext.set("keys.active", newActiveKeys, false);
        evolvContext.set("variants.active", newActiveVariants, false);

        // TODO: 01.06.2021 understand the data type  emit(,,?)
        waitForIt.emit(evolvContext, EFFECTIVE_GENOME_UPDATED, null);

        // TODO: 01.06.2021 add subscriptions
        reevaluatingContext = false;
    }

    private void clearActiveKeys() {
        for (String s : activeKeys.keySet()) activeKeys.remove(s);
    }

    private void clearActiveVariants() {
        for (String s : activeVariants.keySet()) activeVariants.remove(s);
    }

    private Object generateEffectiveGenome(JsonArray expsKeyStates, JsonObject genomes) {

        JsonObject effectiveGenome = new JsonObject();
        JsonArray activeEids = new JsonArray();

        for (JsonElement expKeyStates : expsKeyStates) {
            //JsonObject active = expKeyStates.getAsJsonObject().get("active").getAsJsonObject();
            //JsonObject eid = expKeyStates.getAsJsonObject().get("eid").getAsJsonObject();

//            if (genomes.has("eid") && active) {
//
//            }

        }

        return null;
    }

    // TODO: 30.06.2021 need a unit test
    private void setActiveAndEntryKeyStates(int version,
                                            EvolvContext evolvContext,
                                            JsonObject config,
                                            JsonArray allocations,
                                            KeyStates configKeyStates) {

        JsonArray results = evaluatePredicates(version, evolvContext, config);

        for (JsonElement result : results) {
            //for (Map.Entry<String, JsonElement> entry : result.getAsJsonObject().entrySet()) {
            String eid = "";
            JsonObject expResults = null;
            for (Map.Entry<String, JsonElement> entry : result.getAsJsonObject().entrySet()) {
                eid = getEID(entry.getKey());
                expResults = entry.getValue().getAsJsonObject();
                break;
            }



                //String eid = getEID(entry.getKey());
                //JsonObject expResults = entry.getValue().getAsJsonObject();
                JsonObject expObject = null;
                for (JsonElement experiment : configKeyStates.experiments) {
                    if (experiment.getAsJsonObject().has("\"" + eid + "\"")) {
                        expObject = experiment.getAsJsonObject();
                        break;
                    }
                }
                JsonArray expConfigKeyStates = expObject.get("\"" + eid + "\"").getAsJsonArray();
                JsonObject activeKeyStates = new JsonObject();
                JsonObject entryKeyStates = new JsonObject();

                if (expConfigKeyStates.isJsonNull()) {
                    return;
                }

                for (JsonElement expConfigKeyStateEntry : expConfigKeyStates) {

                    JsonArray expConfigLoaded = expConfigKeyStateEntry.getAsJsonObject().get("loaded").getAsJsonArray();

                    JsonObject loadedKeys = new JsonObject();
                    for (JsonElement key : expConfigLoaded) {
                        loadedKeys.addProperty(key.getAsString(), key.getAsString());
                    }

                    JsonObject newExpKeyStates = getActiveAndEntryExperimentKeyStates(expResults, loadedKeys);
                    //active
                    if (newExpKeyStates.get("active").getAsJsonObject().size() != 0) {
                        for (String key : newExpKeyStates.get("active").getAsJsonObject().keySet()) {
                            activeKeyStates.addProperty("active_" + key,key);
                        }
                    }
                    //entry
                    if (newExpKeyStates.get("entry").getAsJsonObject().size() != 0) {
                        for (String key : newExpKeyStates.get("entry").getAsJsonObject().keySet()) {
                            entryKeyStates.addProperty("entry_" + key,key);
                        }
                    }
                }
                expConfigKeyStates.add(activeKeyStates);
                // TODO: 30.06.2021 add entryKeyStates
                //expConfigKeyStates.set("entry", entryKeyStates);
            //}
        }
    }

    private String getEID(String key) {
        return key.substring(0,key.indexOf("_"));
    }

    private JsonObject getActiveAndEntryExperimentKeyStates(JsonObject results, JsonObject keyStatesLoaded) {

        JsonObject expKeyStates = new JsonObject();

        JsonObject active = new JsonObject();
        JsonObject entry = new JsonObject();

        expKeyStates.add("active", active);
        expKeyStates.add("entry", entry);

        for (String key : keyStatesLoaded.keySet()) {

            boolean activeKey = true;
            //need to get "results.reject" and "results.entry"
            for (Map.Entry<String,JsonElement> rejectedEntry : results.entrySet()) {
                if(key.startsWith(rejectedEntry.getValue().getAsString())){
                    activeKey = false;
                    break;
                }
            }

            if(activeKey){
                active.addProperty(key, key);
                // TODO: 01.07.2021 add entry logic
            }
            
        }

        return expKeyStates;
    }

    private void updateGenome(JsonArray value) {

        JsonArray allocs = new JsonArray();
        JsonArray exclusions = new JsonArray();

        allocs.addAll(value);
        allocations = value;
        genomeFailed = false;

        Iterator<JsonElement> iterator = allocs.iterator();

        while (iterator.hasNext()) {
            JsonObject allocObject = iterator.next().getAsJsonObject();

            if (allocObject.has(GENOME_STRING)) {
                allocObject.remove(GENOME_STRING);
            }
            if (allocObject.has(AUDIENCE_QUERY_STRING)) {
                allocObject.remove(AUDIENCE_QUERY_STRING);
            }
        }
        // TODO: 02.06.2021 calculate exclusions


        evolvContext.set("experiments.allocations", allocs, false);
        evolvContext.set("experiments.exclusions", exclusions, false);
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

        //todo rename variables "jsonObject1" and "jsonObject2" for better understanding
        JsonObject jsonObject1 = new JsonObject();
        JsonObject jsonObject2 = new JsonObject();

        JsonArray expLoaded = new JsonArray();
        JsonArray expMap = new JsonArray();

        jsonObject2.add("loaded", expLoaded);
        expMap.add(jsonObject2);

        jsonObject1.add(String.valueOf(exp.get("id")), expMap);

        keyStates.experiments.add(jsonObject1);

        expLoadedList.clear();
        flattenKeys(clean);

        endsWithFilter();
        // TODO: use a non-depreciated method "JsonParser"
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(expLoadedList.toString()).getAsJsonArray();
        expLoaded.addAll(jsonArray);

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

    private void evaluateAllocationPredicates(EvolvContext evolvContext,
                                              JsonElement allocation,
                                              JsonElement activeKeyStates) {

        // TODO: 02.06.2021 implement
    }

    // TODO: 11.06.2021 need a unit test
    private JsonArray evaluatePredicates(int version, EvolvContext evolvContext, JsonElement config) {
        JsonArray result = new JsonArray();

        if (!config.getAsJsonObject().has("_experiments"))
            if (config.getAsJsonObject().getAsJsonArray("_experiments").size() == 0)
                return result;
        // TODO: 23.06.2021 need to create "context merge" between two contexts
        //JsonElement evaluableContext = evolvContext.resolve();
        JsonElement evaluableContext = ((EvolvContextImpl) evolvContext).getRemoteContext();
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

//            expResult.add("disabled", disabled);
//            expResult.add("entry", entry);
            expResult.add(exp.get("id").getAsString()+"_disabled", disabled);
            expResult.add(exp.get("id").getAsString()+"_entry", entry);

            evaluateBranch(evaluableContext, evaluableConfig, "", disabled, entry);

            result.add(expResult);
        }

        return result;
    }

    // TODO: 11.06.2021 need a unit test
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
                        disabled.addProperty("rejected_"+prefix, prefix);
                        return;
                    }
                }
            }
        }
        if (config.getAsJsonObject().has("_is_entry_point")) {
            if (config.getAsJsonObject().get("_is_entry_point").getAsBoolean()) {
                entry.addProperty("_is_entry_point", prefix);
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

    private void getActiveKeys() {
        // TODO: 25.06.2021 implement
    }
}
