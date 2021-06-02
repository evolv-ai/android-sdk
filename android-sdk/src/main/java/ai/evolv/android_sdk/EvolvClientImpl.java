package ai.evolv.android_sdk;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.generics.GenericClass;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_INITIALIZED;

public class EvolvClientImpl implements EvolvClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientImpl.class);

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
                    WaitForIt waitForIt){

        this.executionQueue = config.getExecutionQueue();
        this.evolvConfig = config;
        this.participant = participant;
        this.waitForIt = waitForIt;
        this.evolvStore = new EvolvStoreImpl(config,participant,waitForIt);
        this.evolvContext = new EvolvContextImpl(evolvStore, waitForIt);
        this.contextBeacon = config.isAnalytics() ? new EvolvEmitter(config.getEndpoint()
                + '/' + config.getEnvironmentId() + "/data",
                evolvContext,
                evolvConfig.isBufferEvents()) : null;
        this.eventBeacon =  new EvolvEmitter(config.getEndpoint()
                + '/' + config.getEnvironmentId() + "/events",
                evolvContext,
                evolvConfig.isBufferEvents());
        //the first time we initialize the context
        initialize(participant.getUserId(),null,null);
    }

    public EvolvContext getEvolvContext() {
        return evolvContext;
    }

    @Override
    public void initialize(String uid, Map<String, Object> remoteContext,  Map<String, Object> localContext) {
        if (initialized) {
            try {
                throw new EvolvKeyError("Evolv: Client is already initialized");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        if (uid.isEmpty()) {
            try {
                throw new EvolvKeyError("Evolv: "+ uid + " must be specified");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        evolvContext.initialize(uid, remoteContext, localContext);
        evolvStore.initialize(evolvContext);

        // TODO: 31.05.2021 add

        if (evolvConfig.isAnalytics()) {

            waitForIt.waitFor(evolvContext, CONTEXT_INITIALIZED, new EvolvInvocation<Object>() {
                @Override
                public void invoke(Object value) {
                //contextBeacon.emit(type, context.remoteContext);
                contextBeacon.emit(value.toString(),null,false);

                }
        });

//            waitForIt.waitFor(evolvContext, CONTEXT_INITIALIZED, function (type, ctx) {
//                contextBeacon.emit(type, context.remoteContext);
//            });
            
        }
    }

    @Override
    public <T> T get(String key, T defaultValue) {
        return null;
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
    public <T> T getActiveKeys(String prefix, T defaultValue) {
        return null;
    }

}
