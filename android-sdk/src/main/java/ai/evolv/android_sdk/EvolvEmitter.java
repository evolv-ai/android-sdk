package ai.evolv.android_sdk;

import android.telecom.TelecomManager;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import okhttp3.MediaType;
import okhttp3.RequestBody;

class EvolvEmitter {

    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static int SCHEDULED_EXECUTOR_TIME = 3000;
    static int FIRST_ARRAY_INDEX = 0;

    private final String endpoint;
    private final EvolvContext evolvContext;
    private final boolean blockTransmit;
    private JsonArray messages = new JsonArray();
    private int timer;
    private final EvolvConfig evolvConfig;
    private final EvolvParticipant participant;
    private final DataCache dataCache = new DataCache(50);

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(true);

    public EvolvEmitter(EvolvConfig evolvConfig, EvolvContext evolvContext, String action, EvolvParticipant participant) {

        this.endpoint = evolvConfig.getEndpoint() + "/" + evolvConfig.getEnvironmentId() + "/" + action;
        this.evolvContext = evolvContext;
        this.evolvConfig = evolvConfig;
        this.participant = participant;
        this.blockTransmit = evolvConfig.isBufferEvents();

    }

    public void setMessages(JsonArray messages) {
        this.messages = messages;
    }

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

    void transmit() {

        boolean sync = false;
        if (messages.size() == 0 || blockTransmit) {
            return;
        }

        JsonArray batch = messages.deepCopy();
        clearMessages(messages);

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
            JsonArray smallBatch = batch.deepCopy();
            if (smallBatch.size() == 0) {
                return;
            }

            dataCache.putEntry(smallBatch);

            if (atomicBoolean.get()) {
                atomicBoolean.set(false);

                ScheduledExecutorService service = evolvConfig.getScheduledExecutorService();
                service.schedule(() -> {
                    sendPreparation(sync);
                    atomicBoolean.set(true);
                }, SCHEDULED_EXECUTOR_TIME, TimeUnit.MILLISECONDS);
            }
        }
    }

    void sendPreparation(boolean sync) {
        RequestBody formBody = wrapMessagesData(dataCache.getCacheArray());
        send(endpoint, formBody, sync);
        dataCache.clearCacheArray();
    }

    private RequestBody wrapMessagesData(JsonArray msgArray) {
        Gson gson = new Gson();
        String messages = gson.toJson(msgArray);
        String uid = gson.toJson(participant.getUserId());

        return RequestBody.create("{\"uid\": " + uid +
                ",\"messages\":" + messages + " }", JSON);
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

        return RequestBody.create("{"
                + "\"uid\":" + uid
                + ",\"cid\":" + cid
                + ",\"eid\":" + eid
                + ",\"type\":" + type
                + contaminationReasonString
                + ",\"timestamp\":" + timestamp + " }", JSON);
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

    private void clearMessages(JsonArray msgs) {
        List<JsonElement> keys = new ArrayList<>();
        for (JsonElement s : msgs) {
            keys.add(s);
        }

        for (JsonElement key : keys) {
            msgs.remove(key);
        }
    }

    private static class DataCache {

        private final JsonArray cacheArray;

        DataCache(int capacity) {
            this.cacheArray = new JsonArray(capacity);
        }

        JsonArray getCacheArray() {
            return cacheArray;
        }

        int sizeCacheArray() {
            return cacheArray.size();
        }

        void putEntry(JsonElement element) {
            cacheArray.add(getCurrentJsonObject(element));
        }

        private JsonObject getCurrentJsonObject(JsonElement element) {
            if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                if (jsonArray.size() != 0) {
                    return jsonArray.get(FIRST_ARRAY_INDEX).getAsJsonObject();
                }
            }
            return new JsonObject();
        }

        void clearCacheArray() {
            List<JsonElement> keys = new ArrayList<>();
            for (JsonElement s : cacheArray) {
                keys.add(s);
            }

            for (JsonElement key : keys) {
                cacheArray.remove(key);
            }
        }
    }
}