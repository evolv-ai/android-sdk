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


    enum AllocationStatus {
        FETCHING, RETRIEVED, FAILED
    }

    enum ConfigurationStatus {
        FETCHING, RETRIEVED, FAILED
    }

    private final ExecutionQueue executionQueue;
    private final EvolvConfig config;
    private final EvolvParticipant participant;
    private final HttpClient httpClient;

    private boolean confirmationSandbagged = false;
    private boolean contaminationSandbagged = false;

    private AllocationStatus allocationStatus;
    private ConfigurationStatus configurationStatus;

    Allocator(EvolvConfig config, EvolvParticipant participant) {
        this.executionQueue = config.getExecutionQueue();
        this.config = config;
        this.participant = participant;
        this.httpClient = config.getHttpClient();
        this.allocationStatus = AllocationStatus.FETCHING;

    }



    AllocationStatus getAllocationStatus() {
        return allocationStatus;
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


    static boolean allocationsNotEmpty(JsonArray allocations) {
        return allocations != null && allocations.size() > 0;
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
