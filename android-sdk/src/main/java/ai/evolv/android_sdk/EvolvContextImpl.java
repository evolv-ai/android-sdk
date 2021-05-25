package ai.evolv.android_sdk;

import java.util.HashMap;
import java.util.Map;

import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.helper.UtilityHelper;

class EvolvContextImpl {

    private String uid;
    private String sid;
    private Map<String, Object> remoteContext;
    private Map<String, Object> localContext;
    private UtilityHelper helper;
    private boolean initialized = false;
    private EvolvConfig evolvConfig;


    public EvolvContextImpl(UtilityHelper helper) {
        this.helper = helper;
    }

    private void initialize(String uid,
                            String  sid,
                            Map<String,String> remoteContext,
                            Map<String,String> localContext){


        if(initialized){
            try {
                throw new EvolvKeyError("Evolv: The context is already initialized");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        this.uid = uid;
        this.sid = sid;
        Class genericRemoteContextClass = remoteContext.getClass();
        Class genericLocalContextClass = remoteContext.getClass();
        this.remoteContext = remoteContext != null ? helper
                .deepCopy(remoteContext,genericRemoteContextClass) : new HashMap<>();
        this.localContext = localContext != null ? helper
                .deepCopy(localContext,genericLocalContextClass) : new HashMap<>();
        initialized = true;

        // TODO: 25.05.2021 emit
        //emit(this, CONTEXT_INITIALIZED, this.resolve());

    }
}
