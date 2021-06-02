package ai.evolv.android_sdk;

import android.util.Log;

import java.util.List;
import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;

class EvolvEmitter {

    private String endpoint;
    private EvolvContext evolvContext;
    private boolean blockTransmit;

    public EvolvEmitter(String endpoint, EvolvContext evolvContext, boolean blockTransmit) {

        this.endpoint = endpoint;
        this.evolvContext = evolvContext;
        this.blockTransmit = blockTransmit;

    }

    void emit(String type, Map<String, List<Object>> payload, boolean flush){

        Log.d("logd_beacon_emit", "emit: !");

    }
}
