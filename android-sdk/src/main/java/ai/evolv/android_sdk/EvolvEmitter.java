package ai.evolv.android_sdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;

class EvolvEmitter {

    private Pattern pattern = Pattern.compile("-?\\\\d+");

    private String endpoint;
    private EvolvContext evolvContext;
    private boolean blockTransmit;
    private JsonObject messages = new JsonObject();
    private int timer;
    private final Handler handler;
    private EvolvConfig evolvConfig;
    private EvolvParticipant participant;
    private Matcher endpointMatch;
    // TODO: 09.07.2021  
    //private boolean v1Events = endpointMatch && endpointMatch[1] === 'v1' && endpointMatch[2] === 'events';

    public EvolvEmitter(EvolvConfig evolvConfig, EvolvContext evolvContext, String action, EvolvParticipant participant) {
        this.endpoint = evolvConfig.getEndpoint() + "/" + evolvConfig.getEnvironmentId() + "/" + action;
        this.evolvContext = evolvContext;
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.blockTransmit = evolvConfig.isBufferEvents();
        handler = new Handler(Looper.getMainLooper());
        endpointMatch = pattern.matcher(endpoint);
        // TODO: 09.07.2021 remove (it's for testing)
        send(endpoint,"",false);
    }

    boolean send(String url, String data, boolean sync) {


        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        // TODO: use a non-depreciated method "create"
        RequestBody formBody = RequestBody.create(JSON, "{\n" +
                "                \"uid\": \"79211876_161787964815811122233309\",\n" +
                "                \"messages\":[{\"type\":\"context.value.added\",\n" +
                "                \"sid\":\"8115094_1625817930138\",\n" +
                "                \"timestamp\":1625826742703}]\n" +
                "\n" +
                "}");

        ListenableFuture<String> responseFuture = evolvConfig.getHttpClient().post(url,formBody);

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    List<Object> requestedKeys = new ArrayList<>();
                    // TODO: use a non-depreciated method "JsonParser"
                    JsonParser parser = new JsonParser();
                    JsonArray allocations = parser.parse(responseFuture.get()).getAsJsonArray();

                } catch (Exception e) {
                    Log.d("EvolvEmitter_evolv", "There was a failure while retrieving the allocations.", e);

                }
            }
        }, MoreExecutors.directExecutor());

        return false;
    }



    void emit(String type, Map<String, List<Object>> payload, boolean flush){
        // TODO: use a non-depreciated method "JsonParser"
        JsonParser parser = new JsonParser();
        JsonObject payloadObject = parser.parse(payload.toString()).getAsJsonObject();

        messages.addProperty("type",type);
        messages.add("payload",payloadObject);
        messages.addProperty("timestamp",new Date().getTime());

        if (flush) {
            transmit();
            return;
        }

        handler.postDelayed(this::transmit, 100);
    }

    private void transmit() {

        boolean sync = false;
        if (messages.size() != 0 || blockTransmit) {
            return;
        }

        JsonObject batch = messages.deepCopy();
        clearMessages();

        if(timer != 0){
            clearTimeout();
        }
// TODO: 09.07.2021  
//        if (v1Events) {
//
//        }else{
//
//        }


    }

    private void clearTimeout() {
        timer = 0;
    }

    private void clearMessages() {
        for (String key : messages.keySet()) messages.remove(key);
    }
}
