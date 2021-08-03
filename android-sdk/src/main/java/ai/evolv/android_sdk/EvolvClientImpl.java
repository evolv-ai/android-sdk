package ai.evolv.android_sdk;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvCallBack;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_INITIALIZED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_ADDED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_CHANGED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_REMOVED;
import static ai.evolv.android_sdk.EvolvStoreImpl.EFFECTIVE_GENOME_UPDATED;
import static ai.evolv.android_sdk.EvolvStoreImpl.EMPTY_STRING;
import static ai.evolv.android_sdk.EvolvStoreImpl.REQUEST_FAILED;

public class EvolvClientImpl implements EvolvClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientImpl.class);

    public static String INITIALIZED = "initialized";
    public static String CONFIRMED = "confirmed";
    public static String CONTAMINATED = "contaminated";
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
        this.contextBeacon = config.isAnalytics() ? new EvolvEmitter(config,
                evolvContext, "data", participant) : null;
        this.eventBeacon = new EvolvEmitter(config, evolvContext, "events",participant);
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

            waitForIt.waitFor(evolvContext, CONTEXT_INITIALIZED, (EvolvInvocation<JsonObject>) type -> {
                JsonObject payloadMap = type;
                contextBeacon.emit(CONTEXT_INITIALIZED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_ADDED, (EvolvInvocation<JsonObject>) type -> {
                if (type.has("local")) {
                    if (type.get("local").getAsBoolean()) {
                        return;
                    }
                }
                JsonObject payloadMap = type;

                contextBeacon.emit(CONTEXT_VALUE_ADDED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_CHANGED, (EvolvInvocation<JsonObject>) type -> {

                if (type.has("local")) {
                    if (type.get("local").getAsBoolean()) {
                        return;
                    }
                }

                JsonObject payloadMap = type;

                contextBeacon.emit(CONTEXT_VALUE_CHANGED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_REMOVED, (EvolvInvocation<JsonObject>) type -> {

                if (type.has("local")) {
                    if (type.get("local").getAsBoolean()) {
                        return;
                    }
                }

                JsonObject payloadMap = type;

                contextBeacon.emit(CONTEXT_VALUE_REMOVED, payloadMap, false);
            });

            if (evolvConfig.isAutoConfirm()) {
                this.confirm();
            }

            initialized = true;
            waitForIt.emit(evolvContext, INITIALIZED, evolvConfig);
        }
    }

    @Override
    public JsonElement get(String key) {

        JsonElement element = evolvStore.getValue(key);

        if (element == null) return JsonNull.INSTANCE;

        if (evolvStore.getValue(key).isJsonPrimitive()) {
            return evolvStore.getValue(key).getAsJsonPrimitive();
        } else if (evolvStore.getValue(key).isJsonObject()) {
            return evolvStore.getValue(key).getAsJsonObject();
        }
        return JsonNull.INSTANCE;
    }

    @Override
    public <T> void subscribe(String key, T defaultValue, EvolvAction<T> function) {


    }

    @Override
    public void confirm() {

        waitForIt.waitFor(evolvContext, EFFECTIVE_GENOME_UPDATED, new EvolvInvocation() {
            @Override
            public void invoke(Object value) {
                JsonObject remoteContext = ((EvolvContextImpl) evolvContext).getRemoteContext();
                JsonElement allocations = JsonNull.INSTANCE;
                if (remoteContext.has("experiments")) {
                    allocations = remoteContext.get("experiments");
                }

                if (allocations.equals(JsonNull.INSTANCE)
                        || evolvStore.config.size() == 0
                        || allocations.getAsJsonObject().size() == 0) {
                    return;
                }

                JsonArray entryPointEids = evolvStore.activeEntryPoints();

                if (entryPointEids.size() == 0) {
                    return;
                }

                JsonObject confirmations = new JsonObject();
                if (remoteContext.has("confirmations")) {
                    confirmations = remoteContext.get("confirmations").getAsJsonObject();
                }

                JsonObject confirmedCids = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : confirmations.entrySet() ) {
                    confirmedCids = entry.getValue().getAsJsonObject().get("cid").getAsJsonObject();
                }

                JsonObject contaminations = new JsonObject();
                if (remoteContext.has("contaminations")) {
                    contaminations = remoteContext.get("contaminations").getAsJsonObject();
                }

                JsonObject contaminatedCids = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : contaminations.entrySet() ) {
                    contaminatedCids = entry.getValue().getAsJsonObject().get("cid").getAsJsonObject();
                }

                JsonArray confirmableAllocations = new JsonArray();
                for (Map.Entry<String, JsonElement> alloc : allocations.getAsJsonObject().get("allocations").getAsJsonObject().entrySet()) {

                    String cid = alloc.getValue().getAsJsonObject().get("cid").getAsString();
                    String eid = alloc.getValue().getAsJsonObject().get("eid").getAsString();

                    if (!confirmedCids.has(cid)
                            && !contaminatedCids.has(cid)
                            && hasEntryPointEids(evolvStore.activeEids, eid)) {
                        confirmableAllocations.add(alloc.getValue());
                    }
                }

                if (confirmableAllocations.size() == 0) {
                    return;
                }

                long timestamp = (new Date()).getTime();
                JsonObject contextConfirmations = new JsonObject();

                for (JsonElement alloc : confirmableAllocations) {
                    contextConfirmations.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());
                    contextConfirmations.addProperty("timestamp", timestamp);
                }

                contextConfirmations.add("confirmations", confirmations);
                JsonObject newConfirmations = contextConfirmations.deepCopy();

                JsonObject updateObject = new JsonObject();
                updateObject.add("confirmations", newConfirmations);
                updateObject.add("experiments.confirmations", newConfirmations);

                evolvContext.update(updateObject, false);

                for (JsonElement alloc : confirmableAllocations) {

                    JsonObject payload = new JsonObject();
                    payload.addProperty("uid", alloc.getAsJsonObject().get("uid").getAsString());
                    payload.addProperty("eid", alloc.getAsJsonObject().get("eid").getAsString());
                    payload.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());

                    eventBeacon.emit("confirmation", payload, false);
                }

                eventBeacon.flush();

                JsonObject object = new JsonObject();
                waitForIt.emit(evolvContext, CONFIRMED, object);
            }
        });
    }

    @Override
    public void contaminate(JsonObject details, boolean allExperiments) {
        JsonObject remoteContext = ((EvolvContextImpl) evolvContext).getRemoteContext();
        JsonElement allocations = JsonNull.INSTANCE;
        if (remoteContext.has("experiments")) {
            allocations = remoteContext.get("experiments");
        }

        if (allocations.equals(JsonNull.INSTANCE)
                || allocations.getAsJsonObject().size() == 0) {
            return;
        }

        if (!details.has("reason")) {
            if (details.get("reason").getAsJsonObject().size() == 0) {
                try {
                    throw new EvolvKeyError("Evolv: contamination details must include a reason");
                } catch (EvolvKeyError evolvKeyError) {
                    evolvKeyError.printStackTrace();
                }
            }
        }

        JsonObject contaminations = new JsonObject();
        if (remoteContext.has("contaminations")) {
            contaminations = remoteContext.get("contaminations").getAsJsonObject();
        }

        JsonObject contaminatedCids = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : contaminations.entrySet() ) {
            contaminatedCids = entry.getValue().getAsJsonObject().get("cid").getAsJsonObject();
        }

        JsonArray contaminatableAllocations = new JsonArray();
        for (Map.Entry<String, JsonElement> alloc : allocations.getAsJsonObject().get("allocations").getAsJsonObject().entrySet()) {

            String cid = alloc.getValue().getAsJsonObject().get("cid").getAsString();
            String eid = alloc.getValue().getAsJsonObject().get("eid").getAsString();
            if (!contaminatedCids.has(cid)
                    && (allExperiments || hasEntryPointEids(evolvStore.activeEids, eid))) {
                contaminatableAllocations.add(alloc.getValue());
            }
        }

        if (contaminatableAllocations.size() == 0) {
            return;
        }

        long timestamp = (new Date()).getTime();
        JsonObject contextContaminations = new JsonObject();

        for (JsonElement alloc : contaminatableAllocations) {
            contextContaminations.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());
            contextContaminations.addProperty("timestamp", timestamp);
        }

        contextContaminations.add("contaminations", contaminations);
        JsonObject newContaminations = contextContaminations.deepCopy();

        JsonObject updateObject = new JsonObject();
        updateObject.add("contaminations", newContaminations);
        updateObject.add("experiments.contaminations", newContaminations);

        evolvContext.update(updateObject, false);

        for (JsonElement alloc : contaminatableAllocations) {

            JsonObject payload = new JsonObject();
            payload.addProperty("uid", alloc.getAsJsonObject().get("uid").getAsString());
            payload.addProperty("eid", alloc.getAsJsonObject().get("eid").getAsString());
            payload.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());
            payload.add("contaminationReason", details);


            eventBeacon.emit("contamination", payload, false);

        }

        eventBeacon.flush();

        JsonObject object = new JsonObject();
        waitForIt.emit(evolvContext, CONTAMINATED, object);
    }

    private boolean hasEntryPointEids(JsonArray entryPointEids, String eid) {

        for (JsonElement entryEid : entryPointEids) {
            if (entryEid.getAsString().equals(eid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JsonObject getActiveKeys(String prefix) {
        return evolvStore.getActiveKeys(prefix);
    }

    @Override
    public JsonObject getActiveKeys() {
        return evolvStore.getActiveKeys();
    }

    @Override
    public void subscribeActiveKeys(String prefix, EvolvAction action) {

        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {
                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.getActiveKeys, prefix.isEmpty() ? EMPTY_STRING : prefix, evolvCallBack);
    }

    @Override
    public void subscribeGet(String key, String defaultValue, EvolvAction action) {
        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {

                if(object == null || object == JsonNull.INSTANCE){
                    JsonPrimitive jsonPrimitive = new JsonPrimitive(defaultValue);
                    object = jsonPrimitive;
                }
                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.get, key, evolvCallBack);
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
    public JsonElement activeEntryPoints() {
        return evolvStore.activeEntryPoints();
    }

    @Override
    public void subscribeIsActive(String key, EvolvAction action) {
        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {
                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.isActive, key, evolvCallBack);
    }

    @Override
    public void subscribeActiveEntryPoints(EvolvAction action) {
        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {
                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.activeEntryPoints, EMPTY_STRING, evolvCallBack);
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

    // TODO: 24.07.2021 an explanation is needed here because it does not work in the js SDK
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
