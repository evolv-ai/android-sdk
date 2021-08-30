package ai.evolv.android_sdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import ai.evolv.android_sdk.httpclients.HttpClient;

public class EvolvConfig {

    static final String DEFAULT_HTTP_SCHEME = "https";
    static final String PROD_DOMAIN = "participants.evolv.ai";
    static final String TEST_DOMAIN = "participants-stg.evolv.ai";
    static final String DEFAULT_DOMAIN = PROD_DOMAIN;
    static final String DEFAULT_ENDPOINT = "https://participants.evolv.ai/v";
    static final int DEFAULT_VERSION = 1;

    private final String httpScheme;
    private final String domain;
    private final int version;
    private final String environmentId;
    private final String endpoint;
    private final HttpClient httpClient;
    private final boolean autoConfirm = false;
    private final boolean analytics = true;
    private final boolean bufferEvents = false;
    private final ExecutorService executorService;
    private final ScheduledExecutorService executorScheduled;

    private EvolvConfig(String httpScheme, String domain, int version,
                        String environmentId,
                        String endpoint,
                        HttpClient httpClient) {
        this.httpScheme = httpScheme;
        this.domain = domain;
        this.version = version;
        this.environmentId = environmentId;
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.executorService = getCachedThreadPool();
        this.executorScheduled = getScheduledThreadPool();
    }

    public static Builder builder(String environmentId, HttpClient httpClient) {
        return new Builder(environmentId, httpClient);
    }

    private ExecutorService getCachedThreadPool() {
        return Executors.newCachedThreadPool();
    }

    private ScheduledExecutorService getScheduledThreadPool() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return executorScheduled;
    }

    String getHttpScheme() {
        return httpScheme;
    }

    String getDomain() {
        return domain;
    }

    int getVersion() {
        return version;
    }

    String getEnvironmentId() {
        return environmentId;
    }

    HttpClient getHttpClient() {
        return this.httpClient;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    public boolean isAnalytics() {
        return analytics;
    }

    public boolean isBufferEvents() {
        return bufferEvents;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public static class Builder {

        private String httpScheme = DEFAULT_HTTP_SCHEME;
        private String domain = DEFAULT_DOMAIN;
        private String endpoint = DEFAULT_ENDPOINT;
        private int version;

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
        public Builder setVersion(int version) {
            this.version = version;
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
         * Builds an instance of EvolvClientImpl.
         * @return an EvolvClientImpl instance
         */
        public EvolvConfig build() {

            if(version == 0){
                version = DEFAULT_VERSION;
            }

            endpoint = DEFAULT_ENDPOINT + version;

            return new EvolvConfig(httpScheme, domain, version, environmentId,
                    endpoint,
                    httpClient);
        }
    }
}
