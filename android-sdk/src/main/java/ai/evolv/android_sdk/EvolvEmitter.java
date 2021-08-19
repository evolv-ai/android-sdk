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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

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
    private EvolvConfig evolvConfig;
    private EvolvParticipant participant;
    private JsonArray dataCollectionMsgs = new JsonArray();


    public EvolvEmitter(EvolvConfig evolvConfig, EvolvContext evolvContext, String action, EvolvParticipant participant) {

        this.endpoint = evolvConfig.getEndpoint() + "/" + evolvConfig.getEnvironmentId() + "/" + action;
        this.evolvContext = evolvContext;
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.blockTransmit = evolvConfig.isBufferEvents();

    }

    // TODO: 16.07.2021 neet unit test
    boolean send(String url, RequestBody formBody, boolean sync) {

        ListenableFuture<String> responseFuture = evolvConfig.getHttpClient().post(url, formBody);

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("EvolvEmitter_response", "response: " + responseFuture.toString());
                } catch (Exception e) {
                    Log.d("EvolvEmitter_data", "There was a failure while retrieving the allocations.", e);
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
        // TODO: 21.07.2021 remove sid, when the server will not "swear" at the lack of the "sid" field in the body
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
    void transmit() {

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
                send(endpoint, formBody, sync);
            }
        } else {
            while (true) {
                // TODO: 12.07.2021 copy a part of array
                JsonArray smallBatch = batch.deepCopy();//.slice(0, BATCH_SIZE);
                if (smallBatch.size() == 0) {
                    break;
                }

//                dataCollectionMsgs.add(smallBatch);

//                boolean allDone = true;
//
//                if(EvolvStoreImpl.futureList == null){
//                    Log.d("count_cust", "1" );
//
//                    break;
//                }
//
//                for (Future<?> future : EvolvStoreImpl.futureList) {
//                    if(!future.isDone()){
//                        allDone = false;
//                    }
//                }
//
//                if(allDone){
//                    RequestBody formBody = wrapMessagesData(smallBatch);
//                    Log.d("count_cust", "3" );
//
//                    send(endpoint, formBody, sync);
//                }

                RequestBody formBody = wrapMessagesData(smallBatch);

                send(endpoint, formBody, sync);

                break;
            }
        }
    }

    private RequestBody wrapMessagesData(JsonArray msgArray) {
        Gson gson = new Gson();
        String messages = gson.toJson(msgArray);
        String uid = gson.toJson(participant.getUserId());
        RequestBody formBody = RequestBody.create(JSON, "{\"uid\": " + uid +
                ",\"messages\":" + messages + " }");


        Log.d("EvolvEmitter_data_thrds", "" + Thread.currentThread().getName());

        Log.d("EvolvEmitter_data", "1: " + "{\"uid\": " + uid +
                ",\"messages\":" + messages + " }");

        return formBody;
    }

    RequestBody wrapMessagesEvents(JsonObject msgObject) {
        Gson gson = new Gson();
        JsonObject payload = msgObject.get("payload").getAsJsonObject();
        String uid = gson.toJson(payload.get("uid"));
        String cid = gson.toJson(payload.get("cid"));
        String eid = gson.toJson(payload.get("eid"));
        String type = gson.toJson(msgObject.get("type"));
        String contaminationReason = gson.toJson(payload.get("contaminationReason"));
        String timestamp = gson.toJson(msgObject.get("timestamp"));

        String contaminationReasonString = "";
        if (payload.get("contaminationReason") != null) {
            contaminationReasonString = ",\"contaminationReason\":" + contaminationReason;
        }

        RequestBody formBody = RequestBody.create(JSON, "{"
                + "\"uid\":" + uid
                + ",\"cid\":" + cid
                + ",\"eid\":" + eid
                + ",\"type\":" + type
                + contaminationReasonString
                + ",\"timestamp\":" + timestamp + " }");

//        Log.d("EvolvEmitter_events", "1: " + "{" + "\n"
//                + "\"uid\":" + uid + "\n"
//                + "\"cid\":" + cid + "\n"
//                + "\"eid\":" + eid + "\n"
//                + ",\"type\":" + type + "\n"
//                + contaminationReasonString + "\n"
//                + ",\"timestamp\":" + timestamp + " }");

        return formBody;
    }

    private boolean v1Events() {
        return endpoint.contains("/v1/") && endpoint.contains("events");
    }

    private void clearTimeout() {
        timer = 0;
    }

    void flush() {
        transmit();
    }

    private void clearMessages() {
        List<JsonElement> keys = new ArrayList<>();
        for (JsonElement s : messages) {
            keys.add(s);
        }

        for (JsonElement key : keys) {
            messages.remove(key);
        }
    }
}