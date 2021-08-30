package ai.evolv.android_sdk;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.httpclients.HttpClient;
import okhttp3.FormBody;
import okhttp3.RequestBody;


class Allocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Allocator.class);

    private final EvolvConfig config;
    private final EvolvParticipant participant;
    private final HttpClient httpClient;

    Allocator(EvolvConfig config, EvolvParticipant participant) {
        this.config = config;
        this.participant = participant;
        this.httpClient = config.getHttpClient();

    }

    String createAllocationsUrl() {
        try {
            String path = String.format("//%s/%s/%s/allocations", config.getDomain(),
                    "v" + config.getVersion(),
                    config.getEnvironmentId());
            URI uri = new URI(config.getHttpScheme(), null, path, "", null);
            URL url = uri.toURL();
            return url.toString();
        } catch (Exception e) {
            LOGGER.error("There was an issue creating the allocations url.", e);
            return "";
        }
    }

    ListenableFuture<String> fetchAllocations() {

        RequestBody formBody = new FormBody.Builder()
                .add("uid", participant.getUserId())
                .build();

        ListenableFuture<String> responseFuture = httpClient.post(createAllocationsUrl(), formBody);
        return responseFuture;
    }

    ListenableFuture<String> fetchConfiguration() {
        ListenableFuture<String> responseFuture = httpClient.get(createConfigurationUrl());
        return responseFuture;
    }

    String createConfigurationUrl() {
        try {
            String path = String.format("//%s/%s/%s/%s/configuration.json", config.getDomain(),
                    "v" + config.getVersion(),
                    config.getEnvironmentId(),
                    participant.getUserId());
            URI uri = new URI(config.getHttpScheme(), null, path, "", null);
            URL url = uri.toURL();
            return url.toString();
        } catch (Exception e) {
            LOGGER.error("There was an issue creating the allocations url.", e);
            return "";
        }
    }

}
