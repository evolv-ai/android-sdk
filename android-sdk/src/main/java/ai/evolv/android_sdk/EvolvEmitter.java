package ai.evolv.android_sdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Date;
import java.util.List;
import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;

class EvolvEmitter {

    private String endpoint;
    private EvolvContext evolvContext;
    private boolean blockTransmit;
    private JsonObject messages = new JsonObject();
    private int timer;
    private final Handler handler;

    public EvolvEmitter(String endpoint, EvolvContext evolvContext, boolean blockTransmit) {
        this.endpoint = endpoint;
        this.evolvContext = evolvContext;
        this.blockTransmit = blockTransmit;
        handler = new Handler(Looper.getMainLooper());
    }

    void emit(String type, Map<String, List<Object>> payload, boolean flush){
        JsonParser parser = new JsonParser();
        JsonObject payloadObject = parser.parse(payload.toString()).getAsJsonObject();

        messages.addProperty("type",type);
        messages.add("payload",payloadObject);
        // TODO: 04.06.2021 sid we don't use anymore
        //messages.addProperty("sid",((EvolvContextImpl)evolvContext).getSid());
        messages.addProperty("timestamp",new Date().getTime());

        if (flush) {
            transmit();
            return;
        }

        handler.postDelayed(this::transmit, 100);
    }

    private void transmit() {
        // TODO: 04.06.2021 implement
    }
}
