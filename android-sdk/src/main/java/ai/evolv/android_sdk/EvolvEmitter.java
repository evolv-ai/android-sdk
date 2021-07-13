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

    public final int BATCH_SIZE = 25;

    private String endpoint;
    private EvolvContext evolvContext;
    private boolean blockTransmit;
    private JsonArray messages = new JsonArray();
    private int timer;
    private final Handler handler;
    private EvolvConfig evolvConfig;
    private EvolvParticipant participant;
    // TODO: 09.07.2021  
    //private boolean v1Events = endpointMatch && endpointMatch[1] === 'v1' && endpointMatch[2] === 'events';

    public EvolvEmitter(EvolvConfig evolvConfig, EvolvContext evolvContext, String action, EvolvParticipant participant) {
        this.endpoint = evolvConfig.getEndpoint() + "/" + evolvConfig.getEnvironmentId() + "/" + action;
        this.evolvContext = evolvContext;
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.blockTransmit = evolvConfig.isBufferEvents();
        handler = new Handler(Looper.getMainLooper());
        // TODO: 09.07.2021 remove (it's for testing)
        //send(endpoint,"",false);
        // TODO: 09.07.2021 remove (it's for testing)
        //transmit();
    }

    boolean send(String url, JsonObject data, boolean sync) {

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        // TODO: need to create request body from messages
        RequestBody formBody = RequestBody.create(JSON, "{\n" +
                "                \"uid\": \"79211876_161787964815811122233309\",\n" +
                "                \"messages\":[{\"type\":\"context.value.added\",\n" +
                "                \"sid\":\"8115094_1625817930138\",\n" +
                "                \"timestamp\":1625826742703}]\n" +
                "\n" +
                "}");

        ListenableFuture<String> responseFuture = evolvConfig.getHttpClient().post(url, formBody);

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


    void emit(String type, JsonObject payload, boolean flush) {
        JsonObject messagesObject = new JsonObject();

        messagesObject.addProperty("type", type);
        messagesObject.add("payload", payload);
        messagesObject.addProperty("sid", "sid_remove_from_server");
        messagesObject.addProperty("timestamp", new Date().getTime());

        messages.add(messagesObject);

        if (flush) {
            transmit();
            return;
        }

        transmit();
    }

    private void transmit() {

        boolean sync = false;
        if (messages.size() == 0 || blockTransmit) {
            return;
        }

        JsonArray batch = messages.deepCopy();
        clearMessages();

        if (timer != 0) {
            clearTimeout();
        }
// TODO: 09.07.2021  
        if (v1Events()) {

            for (JsonElement message : batch) {
                JsonObject editedMessage = message.getAsJsonObject();
                if (editedMessage.has("payload")) {
                    editedMessage.add("payload", editedMessage.get("payload"));
                }

                if (editedMessage.has("type")) {
                    editedMessage.add("type", editedMessage.get("type"));
                }

                send(endpoint, editedMessage, sync);
            }

        } else {

            while (true) {
                // TODO: 12.07.2021 copy a part of array
                JsonArray smallBatch = batch.deepCopy();//.slice(0, BATCH_SIZE);
                if (smallBatch.size() == 0) {
                    break;
                }


                //send(endpoint,smallBatch,sync);
                break;

                // TODO: 12.07.2021 figure it out
//                if (!send(endpoint, JSON.stringify(wrapMessages(smallBatch)), sync)) {
//                    messages = batch
//                    console.error('Evolv: Unable to send analytics beacon');
//                    break;
//                }
//
//                batch = batch.slice(BATCH_SIZE);
            }

        }

        if (messages.size() != 0) {
            //handler.postDelayed(this::transmit, 1000);
        }

    }


    private boolean v1Events() {
        return endpoint.contains("/v1/") && endpoint.contains("events");
    }

    private void clearTimeout() {
        timer = 0;
    }

    private void flush() {
        transmit();
    }

    private void clearMessages() {
        // TODO: 12.07.2021
        //for (String key : messages.keySet()) messages.remove(key);
    }
}
