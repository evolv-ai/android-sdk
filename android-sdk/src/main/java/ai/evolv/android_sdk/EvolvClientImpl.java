package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

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

    private final EvolvParticipant participant;
    private final EvolvConfig evolvConfig;
    private final EvolvEmitter contextBeacon;
    private final EvolvEmitter eventBeacon;

    EvolvClientImpl(EvolvConfig config,
                    EvolvParticipant participant,
                    WaitForIt waitForIt) {

        this.evolvConfig = config;
        this.participant = participant;
        this.waitForIt = waitForIt;
        this.evolvStore = new EvolvStoreImpl(config, participant, waitForIt);
        this.evolvContext = new EvolvContextImpl(evolvStore, waitForIt);
        this.contextBeacon = config.isAnalytics() ? new EvolvEmitter(config,
                evolvContext, "data", participant) : null;
        this.eventBeacon = new EvolvEmitter(config, evolvContext, "events", participant);
        //the first time we initialize the context
        initialize(participant.getUserId(), null, null);
    }

    public EvolvContext getEvolvContext() {
        return evolvContext;
    }

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
                        confirm();
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

        waitForIt.waitFor(evolvContext, EFFECTIVE_GENOME_UPDATED, value -> {
            JsonObject remoteContext = ((EvolvContextImpl) evolvContext).getRemoteContext();
            JsonElement allocations = JsonNull.INSTANCE;

            if (remoteContext.has("experiments")) {
                JsonObject experiments = remoteContext.get("experiments").getAsJsonObject();
                if (experiments.has("allocations")) {
                    allocations = experiments.get("allocations").getAsJsonArray();
                }
            }

            if (allocations.equals(JsonNull.INSTANCE)
                    || evolvStore.config.size() == 0
                    || allocations.getAsJsonArray().size() == 0) {
                return;
            }

            JsonArray entryPointEids = evolvStore.activeEntryPoints();

            if (entryPointEids.size() == 0) {
                return;
            }

            JsonArray confirmations = new JsonArray();
            if (remoteContext.has("experiments")) {
                JsonObject experiments = remoteContext.get("experiments").getAsJsonObject();
                if (experiments.has("confirmations")) {
                    confirmations = experiments.get("confirmations").getAsJsonArray();
                }
            }

            JsonArray confirmedCids = new JsonArray();
            for (JsonElement entry : confirmations) {
                confirmedCids.add(entry.getAsJsonObject().get("cid"));
            }

            JsonArray contaminations = new JsonArray();

            if (remoteContext.has("experiments")) {
                JsonObject experiments = remoteContext.get("experiments").getAsJsonObject();
                if (experiments.has("contaminations")) {
                    contaminations = experiments.get("contaminations").getAsJsonArray();
                }
            }

            JsonArray contaminatedCids = new JsonArray();
            for (JsonElement entry : contaminations) {
                contaminatedCids.add(entry.getAsJsonObject().get("cid"));
            }

            JsonArray confirmableAllocations = new JsonArray();
            for (JsonElement alloc : allocations.getAsJsonArray()) {

                JsonObject allocObject = alloc.getAsJsonObject();
                String cid = allocObject.get("cid").getAsString();
                String eid = allocObject.get("eid").getAsString();

                if (!hasConfirmedCids(confirmedCids, cid)
                        && !hasContaminatedCids(contaminatedCids, cid)
                        && hasEntryPointEids(evolvStore.activeEntryPoints(), eid)) {
                    confirmableAllocations.add(alloc);
                }
            }

            if (confirmableAllocations.size() == 0) {
                return;
            }

            long timestamp = (new Date()).getTime();
            JsonArray contextConfirmations = new JsonArray();

            for (JsonElement alloc : confirmableAllocations) {
                JsonObject contextConfirmationsObject = new JsonObject();
                contextConfirmationsObject.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());
                contextConfirmationsObject.addProperty("timestamp", timestamp);

                contextConfirmations.add(contextConfirmationsObject);
            }

            if (confirmations.size() != 0) {
                contextConfirmations.add(confirmations);
            }
            JsonArray newConfirmations = contextConfirmations.deepCopy();

            JsonObject updateObject = new JsonObject();
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

            waitForIt.emit(evolvContext, CONFIRMED, new JsonObject());
        });
    }

    @Override
    public void contaminate(JsonObject details, boolean allExperiments) {
        JsonObject remoteContext = ((EvolvContextImpl) evolvContext).getRemoteContext();
        JsonElement allocations = JsonNull.INSTANCE;

        if (remoteContext.has("experiments")) {
            JsonObject experiments = remoteContext.get("experiments").getAsJsonObject();
            if (experiments.has("allocations")) {
                allocations = experiments.get("allocations").getAsJsonArray();
            }
        }

        if (allocations.equals(JsonNull.INSTANCE)
                || allocations.getAsJsonArray().size() == 0) {
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

        JsonArray contaminations = new JsonArray();
        if (remoteContext.has("experiments")) {
            JsonObject experiments = remoteContext.get("experiments").getAsJsonObject();
            if (experiments.has("contaminations")) {
                contaminations = experiments.get("contaminations").getAsJsonArray();
            }
        }

        JsonArray contaminatedCids = new JsonArray();
        for (JsonElement entry : contaminations) {
            contaminatedCids.add(entry.getAsJsonObject().get("cid"));
        }

        JsonArray contaminatableAllocations = new JsonArray();
        for (JsonElement alloc : allocations.getAsJsonArray()) {

            JsonObject allocObject = alloc.getAsJsonObject();
            String cid = allocObject.get("cid").getAsString();
            String eid = allocObject.get("eid").getAsString();

            if (!hasContaminatedCids(contaminatedCids, cid)
                    && (allExperiments || hasActiveEids(evolvStore.activeEids, eid))) {
                contaminatableAllocations.add(alloc);
            }
        }

        if (contaminatableAllocations.size() == 0) {
            return;
        }

        long timestamp = (new Date()).getTime();
        JsonArray contextContaminations = new JsonArray();

        for (JsonElement alloc : contaminatableAllocations) {
            JsonObject contextContaminationsObject = new JsonObject();
            contextContaminationsObject.addProperty("cid", alloc.getAsJsonObject().get("cid").getAsString());
            contextContaminationsObject.addProperty("timestamp", timestamp);

            contextContaminations.add(contextContaminationsObject);
        }

        if (contaminations.size() != 0) {
            contextContaminations.add(contaminations);
        }

        JsonArray newContaminations = contextContaminations.deepCopy();

        JsonObject updateObject = new JsonObject();
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
        waitForIt.emit(evolvContext, CONTAMINATED, new JsonObject());
    }

    private boolean hasEntryPointEids(JsonArray entryPoint, String eid) {
        for (JsonElement entry : entryPoint) {
            if (entry.getAsString().equals(eid)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveEids(JsonArray activeEids, String eid) {
        for (JsonElement entryEid : activeEids) {
            for (JsonElement element : entryEid.getAsJsonArray()) {
                if (element.getAsString().equals(eid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasConfirmedCids(JsonArray confirmedCids, String cid) {
        for (JsonElement entryCid : confirmedCids) {
            if (entryCid.getAsString().equals(cid)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasContaminatedCids(JsonArray contaminatedCids, String cid) {
        for (JsonElement entryCid : contaminatedCids) {
            if (entryCid.getAsString().equals(cid)) {
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

                if (object == null || object == JsonNull.INSTANCE) {
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
    public void on(String topic, EvolvInvocation listener) {
        waitForIt.waitFor(evolvContext, topic, listener);
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

    @Override
    public void clearActiveKeys(String prefix) {
        evolvStore.clearActiveKeys(prefix);
    }

    @Override
    public void clearActiveKeys() {
        evolvStore.clearActiveKeys();
    }
}
