package ai.evolv.android_sdk;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import static ai.evolv.android_sdk.EvolvStoreImpl.EMPTY_STRING;
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

            waitForIt.waitFor(evolvContext, CONTEXT_INITIALIZED, (EvolvInvocation<JsonObject>) type -> {
                JsonObject payloadMap = type;
                contextBeacon.emit(CONTEXT_INITIALIZED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_ADDED, (EvolvInvocation<JsonObject>) type -> {
                if(type.has("local")){
                    if(type.get("local").getAsBoolean()){
                        return;
                    }
                }
                JsonObject payloadMap = type;

                contextBeacon.emit(CONTEXT_VALUE_ADDED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_CHANGED, (EvolvInvocation<JsonObject>) type -> {

                if(type.has("local")){
                    if(type.get("local").getAsBoolean()){
                        return;
                    }
                }

                JsonObject payloadMap = type;

                contextBeacon.emit(CONTEXT_VALUE_CHANGED, payloadMap, false);
            });

            waitForIt.waitFor(evolvContext, CONTEXT_VALUE_REMOVED, (EvolvInvocation<JsonObject>) type -> {

                if(type.has("local")){
                    if(type.get("local").getAsBoolean()){
                        return;
                    }
                }

                JsonObject payloadMap = type;

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
// TODO: 19.07.2021 uncomment (callBack testing)
//    @Override
//    public JsonElement get(String key) {
//
//        JsonElement element = evolvStore.getValue(key);
//
//        if (element == null) return JsonNull.INSTANCE;
//
//        if( evolvStore.getValue(key).isJsonPrimitive()){
//            return evolvStore.getValue(key).getAsJsonPrimitive();
//        }else if(evolvStore.getValue(key).isJsonObject()){
//            return evolvStore.getValue(key).getAsJsonObject();
//        }
//        return JsonNull.INSTANCE;
//    }

    @Override
    public void get(String key, EvolvAction action) {

        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {

                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.get, key, evolvCallBack);
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
    // TODO: 19.07.2021 uncomment (callBack testing)
//    @Override
//    public JsonObject getActiveKeys(String prefix) {
//       return  evolvStore.getActiveKeys(prefix);
//    }

    @Override
    public void getActiveKeys(String prefix, EvolvAction action) {

        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {
                Log.d("evolvCallBack_", "4 invoke CLIENT: ");
                action.apply(object);
            }
        };

        //evolvStore.getActiveKeys(prefix, evolvCallBack);
        evolvStore.subscribe(EvolvType.getActiveKeys, prefix, evolvCallBack);

    }

    // TODO: 19.07.2021 uncomment (callBack testing)
//    @Override
//    public JsonObject getActiveKeys() {
//        return evolvStore.getActiveKeys();
//    }
    @Override
    public void getActiveKeys(EvolvAction action) {

        EvolvCallBack evolvCallBack = new EvolvCallBack() {
            @Override
            public void invoke(Object object) {
                Log.d("evolvCallBack_", "4 invoke CLIENT: ");
                action.apply(object);
            }
        };

        evolvStore.subscribe(EvolvType.getActiveKeys,EMPTY_STRING,evolvCallBack);
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
