package ai.evolv.android_sdk;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.httpclients.HttpClient;

public class EvolvConfig {

    static final String DEFAULT_HTTP_SCHEME = "https";
    static final String DEFAULT_DOMAIN = "participants.evolv.ai";
    static final String DEFAULT_API_VERSION = "v1";

    private static final int DEFAULT_ALLOCATION_STORE_SIZE = 1000;

    private final String httpScheme;
    private final String domain;
    private final String version;
    private final String environmentId;
    private final EvolvAllocationStore evolvAllocationStore;
    private final HttpClient httpClient;
    private final ExecutionQueue executionQueue;

    private EvolvConfig(String httpScheme, String domain, String version,
                        String environmentId,
                        EvolvAllocationStore evolvAllocationStore,
                        HttpClient httpClient) {
        this.httpScheme = httpScheme;
        this.domain = domain;
        this.version = version;
        this.environmentId = environmentId;
        this.evolvAllocationStore = evolvAllocationStore;
        this.httpClient = httpClient;
        this.executionQueue = new ExecutionQueue();
    }

    public static Builder builder(String environmentId, HttpClient httpClient) {
        return new Builder(environmentId, httpClient);
    }

    String getHttpScheme() {
        return httpScheme;
    }

    String getDomain() {
        return domain;
    }

    String getVersion() {
        return version;
    }

    String getEnvironmentId() {
        return environmentId;
    }

    EvolvAllocationStore getEvolvAllocationStore() {
        return evolvAllocationStore;
    }

    HttpClient getHttpClient() {
        return this.httpClient;
    }

    ExecutionQueue getExecutionQueue() {
        return this.executionQueue;
    }

    public static class Builder {

        private int allocationStoreSize = DEFAULT_ALLOCATION_STORE_SIZE;
        private String httpScheme = DEFAULT_HTTP_SCHEME;
        private String domain = DEFAULT_DOMAIN;
        private String version = DEFAULT_API_VERSION;
        private EvolvAllocationStore allocationStore;

        private String environmentId;
        private HttpClient httpClient;

        /**
         * Responsible for creating an instance of EvolvClientImpl.
         * <p>
         *     Builds an instance of the EvolvClientImpl. The only required parameter is the
         *     customer's environment id.
         * </p>
         * @param environmentId unique id representing a customer's environment
         */
        Builder(String environmentId, HttpClient httpClient) {
            this.environmentId = environmentId;
            this.httpClient = httpClient;
        }

        /**
         * Sets the domain of the underlying EvolvParticipant api.
         * @param domain the domain of the EvolvParticipant api
         * @return EvolvClientBuilder class
         */
        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * Version of the underlying EvolvParticipant api.
         * @param version representation of the required EvolvParticipant api version
         * @return EvolvClientBuilder class
         */
        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets up a custom EvolvAllocationStore. Store needs to implement the
         * EvolvAllocationStore interface.
         * @param allocationStore a custom built allocation store
         * @return EvolvClientBuilder class
         */
        public Builder setEvolvAllocationStore(EvolvAllocationStore allocationStore) {
            this.allocationStore = allocationStore;
            return this;
        }

        /**
         * Tells the SDK to use either http or https.
         * @param scheme either http or https
         * @return EvolvClientBuilder class
         */
        public Builder setHttpScheme(String scheme) {
            this.httpScheme = scheme;
            return this;
        }

        /**
         * Sets the DefaultAllocationStores size.
         * @param size number of entries allowed in the default allocation store
         * @return EvolvClientBuilder class
         */
        public Builder setDefaultAllocationStoreSize(int size) {
            this.allocationStoreSize = size;
            return this;
        }

        /**
         * Builds an instance of EvolvClientImpl.
         * @return an EvolvClientImpl instance
         */
        public EvolvConfig build() {
            if (allocationStore == null) {
                allocationStore = new DefaultAllocationStore(allocationStoreSize);
            }

            return new EvolvConfig(httpScheme, domain, version, environmentId,
                    allocationStore,
                    httpClient);
        }

    }

}
