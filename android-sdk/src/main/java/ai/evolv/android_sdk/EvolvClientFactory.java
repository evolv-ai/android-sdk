package ai.evolv.android_sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.evolv.android_sdk.evolvinterface.EvolvClient;

public class EvolvClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientFactory.class);

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

        return new EvolvClientImpl(config,
                participant,
                new WaitForIt());
    }
}
