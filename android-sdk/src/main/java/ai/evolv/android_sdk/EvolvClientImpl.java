package ai.evolv.android_sdk;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_INITIALIZED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_ADDED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_CHANGED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_REMOVED;
import static ai.evolv.android_sdk.EvolvStoreImpl.REQUEST_FAILED;

public class EvolvClientImpl implements EvolvClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientImpl.class);

    public static String INITIALIZED = "initialized";
    public static String CONFIRMED = "'confirmed'";
    public static String CONTAMINATED = "'contaminated'";
    public static String EVENT_EMITTED = "event.emitted";

    private boolean initialized = false;
    private EvolvContext evolvContext;
    private EvolvStoreImpl evolvStore;
    private WaitForIt waitForIt;

    private final ExecutionQueue executionQueue;
    private final EvolvParticipant participant;
    private final EvolvConfig evolvConfig;
    private final EvolvEmitter contextBeacon;
    private final EvolvEmitter eventBeacon;

    EvolvClientImpl(EvolvConfig config,
                    EvolvParticipant participant,
                    WaitForIt waitForIt) {

        this.executionQueue = config.getExecutionQueue();
        this.evolvConfig = config;
        this.participant = participant;
        this.waitForIt = waitForIt;
        this.evolvStore = new EvolvStoreImpl(config, participant, waitForIt);
        this.evolvContext = new EvolvContextImpl(evolvStore, waitForIt);
//        this.contextBeacon = config.isAnalytics() ? new EvolvEmitter(config.getEndpoint()
//                + '/' + config.getEnvironmentId() + "/data",
//                evolvContext,
//                evolvConfig.isBufferEvents()) : null;

        this.contextBeacon = config.isAnalytics() ? new EvolvEmitter(config,
                evolvContext, "data",participant) : null;
        // TODO: 09.07.2021 uncomment
        this.eventBeacon = null;//new EvolvEmitter(config, evolvContext, "events",participant);
        //the first time we initialize the context
        initialize(participant.getUserId(), null, null);
    }

    public EvolvContext getEvolvContext() {
        return evolvContext;
    }

    // TODO: 07.07.2021 need to test
    @Override
    public void initialize(String uid, JsonObject remoteContext, JsonObject localContext) {
        if (initialized) {
            try {
                throw new EvolvKeyError("Evolv: Client is already initialized");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        if (uid.isEmpty()) {
            try {
                throw new EvolvKeyError("Evolv: " + uid + " must be specified");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        evolvContext.initialize(uid, remoteContext, localContext);
        evolvStore.initialize(evolvContext);

        // TODO: 31.05.2021 add

        if (evolvConfig.isAnalytics()) {

            waitForIt.waitFor(evolvContext, CONTEXT_INITIALIZED, (EvolvInvocation<Object>) type -> {
                Map<String, List<Object>> payloadMap = new LinkedHashMap<>();
                //todo create list which will include all payload parameters from "waitforit"
                payloadMap.put(CONTEXT_INITIALIZED, new ArrayList<>());

                contextBeacon.emit(CONTEXT_INITIALIZED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_ADDED, (EvolvInvocation<Object>) type -> {
                // TODO: 04.06.2021 note: need to get "local" from anonymous function
                boolean local = true;
                if (local) {
                    return;
                }
                Map<String, List<Object>> payloadMap = new LinkedHashMap<>();
                //todo create list which will include all payload parameters from "waitforit"
                payloadMap.put(CONTEXT_VALUE_ADDED, new ArrayList<>());

                contextBeacon.emit(CONTEXT_VALUE_ADDED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_CHANGED, (EvolvInvocation<Object>) type -> {
                // TODO: 04.06.2021 note: need to get "local" from anonymous function
                boolean local = true;
                if (local) {
                    return;
                }
                Map<String, List<Object>> payloadMap = new LinkedHashMap<>();
                //todo create list which will include all payload parameters from "waitforit"
                payloadMap.put(CONTEXT_VALUE_CHANGED, new ArrayList<>());

                contextBeacon.emit(CONTEXT_VALUE_CHANGED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_REMOVED, (EvolvInvocation<Object>) type -> {
                // TODO: 04.06.2021 note: need to get "local" from anonymous function
                boolean local = true;
                if (local) {
                    return;
                }
                Map<String, List<Object>> payloadMap = new LinkedHashMap<>();
                //todo create list which will include all payload parameters from "waitforit"
                payloadMap.put(CONTEXT_VALUE_REMOVED, new ArrayList<>());

                contextBeacon.emit(CONTEXT_VALUE_REMOVED, payloadMap, false);
            });

            if (evolvConfig.isAutoConfirm()) {
                this.confirm();
                // TODO: 04.06.2021 note: third parameter "this.contaminate.bind(this)" from js SDK
                waitForIt.waitFor(evolvContext, REQUEST_FAILED, null);
            }

            initialized = true;
            waitForIt.emit(evolvContext, INITIALIZED, evolvConfig);
        }
    }

    @Override
    public JsonElement get(String key) {
        return evolvStore.getValue(key);
    }

    @Override
    public <T> void subscribe(String key, T defaultValue, EvolvAction<T> function) {
    }

    @Override
    public void confirm() {
    }

    @Override
    public void contaminate() {
    }

    @Override
    public JsonObject getActiveKeys(String prefix) {
       return  evolvStore.getActiveKeys(prefix);
    }

    @Override
    public JsonObject getActiveKeys() {
        return evolvStore.getActiveKeys();
    }

    @Override
    public void reevaluateContext() {
        evolvStore.reevaluateContext();
    }

    @Override
    public boolean isActive(String key) {
        return evolvStore.getValueActive(key);
    }

    @Override
    public void preload(ArrayList<String> prefixes, boolean configOnly, boolean immediate) {
        evolvStore.preload(prefixes, configOnly, immediate);
    }

    @Override
    public void preload(ArrayList<String> prefixes, boolean configOnly) {
        evolvStore.preload(prefixes, configOnly);
    }

    @Override
    public void preload(ArrayList<String> prefixes) {
        evolvStore.preload(prefixes);
    }

    @Override
    public JsonElement getConfig(String key) {
        return evolvStore.getConfig(key);
    }

    @Override
    public void clearActiveKeys(String prefix) {
        evolvStore.clearActiveKeys(prefix);
    }

    @Override
    public void clearActiveKeys() {
        evolvStore.clearActiveKeys();
    }
}
