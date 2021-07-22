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

import java.util.Date;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import okhttp3.MediaType;
import okhttp3.RequestBody;

class EvolvEmitter {

    public final int BATCH_SIZE = 25;
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String endpoint;
    private EvolvContext evolvContext;
    private boolean blockTransmit;
    private JsonArray messages = new JsonArray();
    private int timer;
    private final Handler handler;
    private EvolvConfig evolvConfig;
    private EvolvParticipant participant;

    public EvolvEmitter(EvolvConfig evolvConfig, EvolvContext evolvContext, String action, EvolvParticipant participant) {

        this.endpoint = evolvConfig.getEndpoint() + "/" + evolvConfig.getEnvironmentId() + "/" + action;
        this.evolvContext = evolvContext;
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.blockTransmit = evolvConfig.isBufferEvents();
        handler = new Handler(Looper.getMainLooper());

    }

    // TODO: 16.07.2021 neet unit test
    boolean send(String url, RequestBody formBody, boolean sync) {

        ListenableFuture<String> responseFuture = evolvConfig.getHttpClient().post(url, formBody);

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("EvolvEmitter_evolv", "RUN: " + responseFuture.toString());
                } catch (Exception e) {
                    Log.d("EvolvEmitter_evolv1", "There was a failure while retrieving the allocations.", e);
                }
            }
        }, MoreExecutors.directExecutor());

        return false;
    }

    // TODO: 16.07.2021 need unit test
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

    // TODO: 16.07.2021 need unit test
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

        if (v1Events()) {
            for (JsonElement message : batch) {
                JsonObject editedMessage = message.getAsJsonObject();
                if (editedMessage.has("payload")) {
                    editedMessage.add("payload", editedMessage.get("payload"));
                }

                if (editedMessage.has("type")) {
                    editedMessage.add("type", editedMessage.get("type"));
                }

                RequestBody formBody = wrapMessagesEvents(editedMessage);
                // TODO: 16.07.2021 uncomment (do not spam the server during testing)
                //send(endpoint, formBody, sync);
            }
        } else {
            while (true) {
                // TODO: 12.07.2021 copy a part of array
                JsonArray smallBatch = batch.deepCopy();//.slice(0, BATCH_SIZE);
                if (smallBatch.size() == 0) {
                    break;
                }

                RequestBody formBody = wrapMessagesData(smallBatch);
                // TODO: 16.07.2021 uncomment (do not spam the server during testing)
                //send(endpoint, formBody, sync);
                break;
// TODO: 15.07.2021 copy a part of array
//                batch = batch.slice(BATCH_SIZE);
            }
        }
    }

    private RequestBody wrapMessagesData(JsonArray msgArray) {
        Gson gson = new Gson();
        String uid = gson.toJson(participant.getUserId());
        String messages = gson.toJson(msgArray);

        RequestBody formBody = RequestBody.create(JSON, "{\"uid\": " + uid +
                ",\"messages\":" + messages + " }");

        return formBody;
    }

    private RequestBody wrapMessagesEvents(JsonObject msgObject) {
        Gson gson = new Gson();
        String messages = gson.toJson(msgObject);

        RequestBody formBody = RequestBody.create(JSON, "{\"messages\":" + messages + " }");

        return formBody;
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
        for (JsonElement key : messages) messages.remove(key);
    }
}