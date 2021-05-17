package ai.evolv.android_sdk;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;

public class EvolvClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientFactory.class);

    /**
     * Creates instances of the EvolvClient.
     *
     * @param config general configurations for the SDK
     * @return an instance of EvolvClient
     */
    public static EvolvClient init(EvolvConfig config) {
        LOGGER.debug("Initializing Evolv Client.");
        EvolvParticipant participant = EvolvParticipant.builder().build();
        return EvolvClientFactory.createClient(config, participant);
    }

    /**
     * Creates instances of the EvolvClient.
     *
     * @param config general configurations for the SDK
     * @param participant the participant for the initialized client
     * @return an instance of EvolvClient
     */
    public static EvolvClient init(EvolvConfig config, EvolvParticipant participant) {
        LOGGER.debug("Initializing Evolv Client.");
        return EvolvClientFactory.createClient(config, participant);
    }

    private static EvolvClient createClient(EvolvConfig config, EvolvParticipant participant) {
        EvolvAllocationStore store = config.getEvolvAllocationStore();
        JsonArray previousAllocations = store.get(participant.getUserId());

        Allocator allocator = new Allocator(config, participant);

        // fetch and reconcile allocations asynchronously
        ListenableFuture<JsonArray> futureAllocations = allocator.fetchAllocations();

        return new EvolvClientImpl(config,
                new EventEmitter(config, participant, store),
                futureAllocations,
                allocator,
                Allocator.allocationsNotEmpty(previousAllocations),
                participant);
    }
}
