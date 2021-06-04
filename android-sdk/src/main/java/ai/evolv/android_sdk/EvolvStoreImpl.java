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
import java.util.function.Consumer;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;

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


    private EvolvConfig evolvConfig;
    ListenableFuture<JsonObject> futureConfiguration;
    ListenableFuture<JsonArray> futureAllocations;
    private boolean initialized = false;
    private EvolvContext evolvContext;
    private EvolvParticipant participant;
    Allocator allocator;
    JsonObject config;
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

    class KeyStates {

        Set needed = new HashSet();
        Set requested = new HashSet();
        Map experiments = new HashMap<>();
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
                updateConfig((JsonObject) value);
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
        Object result = generateEffectiveGenome(configKeyStates.experiments, genomes);

        // TODO: 01.06.2021 add

        evolvContext.set("keys.active", activeKeys, false);
        evolvContext.set("variants.active", activeVariants, false);

        // TODO: 01.06.2021 understand the data type  emit(,,?)
        waitForIt.emit(evolvContext, EFFECTIVE_GENOME_UPDATED, null);

        // TODO: 01.06.2021 add
        reevaluatingContext = false;
    }

    private Object generateEffectiveGenome(Map expsKeyStates, JsonObject genomes) {
        // TODO: 01.06.2021 implement
        return null;
    }

    private void setActiveAndEntryKeyStates(int version,
                                            EvolvContext evolvContext,
                                            JsonObject config,
                                            JsonArray allocations,
                                            KeyStates configKeyStates) {
        // TODO: 01.06.2021 implement

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
        // TODO: 27.05.2021 add
    }

    private void evaluateAllocationPredicates(EvolvContext evolvContext,
                                              JsonElement allocation,
                                              JsonElement activeKeyStates) {

        // TODO: 02.06.2021 implement
    }

    private void evaluatePredicates(int version, EvolvContext evolvContext, JsonElement config) {
        // TODO: 02.06.2021 implement
    }

}
