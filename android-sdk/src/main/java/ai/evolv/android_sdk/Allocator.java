package ai.evolv.android_sdk;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.httpclients.HttpClient;


class Allocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Allocator.class);

    enum AllocationStatus {
        FETCHING, RETRIEVED, FAILED
    }

    private final ExecutionQueue executionQueue;
    private final EvolvAllocationStore store;
    private final EvolvConfig config;
    private final EvolvParticipant participant;
    private final EventEmitter eventEmitter;
    private final HttpClient httpClient;

    private boolean confirmationSandbagged = false;
    private boolean contaminationSandbagged = false;

    private AllocationStatus allocationStatus;

    Allocator(EvolvConfig config, EvolvParticipant participant) {
        this.executionQueue = config.getExecutionQueue();
        this.store = config.getEvolvAllocationStore();
        this.config = config;
        this.participant = participant;
        this.httpClient = config.getHttpClient();
        this.allocationStatus = AllocationStatus.FETCHING;
        this.eventEmitter = new EventEmitter(config, participant, this.store);

    }

    AllocationStatus getAllocationStatus() {
        return allocationStatus;
    }

    void sandBagConfirmation() {
        confirmationSandbagged = true;
    }

    void sandBagContamination() {
        contaminationSandbagged = true;
    }

    String createAllocationsUrl() {
        try {
            String path = String.format("//%s/%s/%s/allocations", config.getDomain(),
                    config.getVersion(),
                    config.getEnvironmentId());
            String queryString = String.format("uid=%s&sid=%s", participant.getUserId(),
                    participant.getSessionId());
            URI uri = new URI(config.getHttpScheme(), null, path, "", null);
            URL url = uri.toURL();
            return url.toString();
        } catch (Exception e) {
            LOGGER.error("There was an issue creating the allocations url.", e);
            return "";
        }
    }

    ListenableFuture<JsonArray> fetchAllocations() {
        ListenableFuture<String> responseFuture = httpClient.post(createAllocationsUrl(),
                participant.getUserAttributes());

        SettableFuture<JsonArray> allocationsFuture = SettableFuture.create();

        responseFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonParser parser = new JsonParser();
                    JsonArray allocations = parser.parse(responseFuture.get()).getAsJsonArray();

                    JsonArray previousAllocations = store.get(participant.getUserId());
                    if (allocationsNotEmpty(previousAllocations)) {
                        allocations = Allocations.reconcileAllocations(previousAllocations, allocations);
                    }

                    store.put(participant.getUserId(), allocations);
                    allocationStatus = AllocationStatus.RETRIEVED;

                    allocationsFuture.set(allocations);

                    executionQueue.executeAllWithValuesFromAllocations(allocations,
                            eventEmitter, confirmationSandbagged, contaminationSandbagged);
                } catch (Exception e) {
                    LOGGER.warn("There was a failure while retrieving the allocations.", e);
                    allocationsFuture.set(resolveAllocationFailure());
                }
            }
        }, MoreExecutors.directExecutor());

        return allocationsFuture;
    }

    JsonArray resolveAllocationFailure() {
        JsonArray previousAllocations = store.get(participant.getUserId());
        if (allocationsNotEmpty(previousAllocations)) {
            LOGGER.debug("Falling back to participant's previous allocation.");
            allocationStatus = AllocationStatus.RETRIEVED;

            executionQueue.executeAllWithValuesFromAllocations(previousAllocations, eventEmitter,
                    confirmationSandbagged, contaminationSandbagged);
        } else {
            LOGGER.debug("Falling back to the supplied defaults.");
            allocationStatus = AllocationStatus.FAILED;
            executionQueue.executeAllWithValuesFromDefaults();
            previousAllocations = new JsonArray();
        }

        return previousAllocations;
    }

    static boolean allocationsNotEmpty(JsonArray allocations) {
        return allocations != null && allocations.size() > 0;
    }
}
