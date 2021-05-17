package ai.evolv.android_sdk;

import ai.evolv.android_sdk.httpclients.HttpClient;



public class Options {

//    public static final String DEFAULT_HTTP_SCHEME = "https";
//    public static final String DEFAULT_DOMAIN_TEST = "participants-stg.evolv.ai";
//    public static final String DEFAULT_DOMAIN_PROD = "participants.evolv.ai";
//    public static final String DEFAULT_API_VERSION = "v1";
//
//    private String environment;
//    private String endpoint;
//    private int version;
//    private boolean autoConfirm;
//    private boolean analytics;
//    private boolean bufferEvents;
//    private HttpClient httpClient;
//    private EvolvAllocationStore evolvAllocationStore;
//    private EvolvContext evolvContext;
//    private EvolvStore evolvStore;
//    private Emitter beacon;
//    private ExecutionQueueMain executionQueue;
//
//    private Options(String environment, String endpoint, int version, boolean autoConfirm,
//                    boolean analytics, boolean bufferEvents,  EvolvAllocationStore evolvAllocationStore,
//                    EvolvContext evolvContext, EvolvStore evolvStore, Emitter beacon, HttpClient httpClient) {
//        this.environment = environment;
//        this.endpoint = endpoint;
//        this.version = version;
//        this.autoConfirm = autoConfirm;
//        this.analytics = analytics;
//        this.bufferEvents = bufferEvents;
//        this.evolvAllocationStore = evolvAllocationStore;
//        this.evolvContext = evolvContext;
//        this.evolvStore = evolvStore;
//        this.beacon = beacon;
//        this.httpClient = httpClient;
//        this.executionQueue = new ExecutionQueueMain();
//    }
//
//    public String getEnvironment() {
//        return environment;
//    }
//
//    public String getEndpoint() {
//        return endpoint;
//    }
//
//    public int getVersion() {
//        return version;
//    }
//
//    public boolean isAutoConfirm() {
//        return autoConfirm;
//    }
//
//    public boolean isAnalytics() {
//        return analytics;
//    }
//
//    public boolean isBufferEvents() {
//        return bufferEvents;
//    }
//
//    public EvolvContext getEvolvContext() {
//        return evolvContext;
//    }
//
//    public EvolvStore getEvolvStore() {
//        return evolvStore;
//    }
//
//    public Emitter getBeacon() {
//        return beacon;
//    }
//
//    public HttpClient getHttpClient() {
//        return httpClient;
//    }
//
//    public EvolvAllocationStore getEvolvAllocationStore() {
//        return evolvAllocationStore;
//    }
//
//    public ExecutionQueueMain getExecutionQueue() {
//        return this.executionQueue;
//    }
//
//    public void setVersion(int version) {
//        this.version = version;
//    }
//
//    public void setEndpoint(String endpoint) {
//        this.endpoint = endpoint;
//    }
//
//    public void setAnalytics(boolean analytics) {
//        this.analytics = analytics;
//    }
//
//    public void setBeacon(Emitter beacon) {
//        this.beacon = beacon;
//    }
//
//    public static class Builder{
//
//        private String environment;
//        private String endpoint;
//        private int version;
//        private boolean autoConfirm;
//        private boolean analytics;
//        private boolean bufferEvents;
//        private HttpClient httpClient;
//        private EvolvContext evolvContext;
//        private EvolvStore evolvStore;
//        private Emitter emitter;
//        private EvolvAllocationStore allocationStore;
//
//        public Builder setEnviroment(String enviroment){
//            this.environment = enviroment;
//            return this;
//        }
//
//        public Builder setEndpoint(String endpoint){
//            this.endpoint = endpoint;
//            return this;
//        }
//
//        public Builder setVersion(int version){
//            this.version = version;
//            return this;
//        }
//
//        public Builder setAutoConfirm(boolean autoConfirm){
//            this.autoConfirm = autoConfirm;
//            return this;
//        }
//
//        public Builder setAnalytics(boolean analytics){
//            this.analytics = analytics;
//            return this;
//        }
//
//        public Builder setEvolvAllocationStore(EvolvAllocationStore allocationStore) {
//            this.allocationStore = allocationStore;
//            return this;
//        }
//
//        public Builder setBufferEvents(boolean bufferEvents){
//            this.bufferEvents = bufferEvents;
//            return this;
//        }
//
//        public Builder setEvolvContext(EvolvContext evolvContext){
//            this.evolvContext = evolvContext;
//            return this;
//        }
//
//        public Builder setEvolvStore(EvolvStore evolvStore){
//            this.evolvStore = evolvStore;
//            return this;
//        }
//
//        public Builder setEmitter(Emitter emitter){
//            this.emitter = emitter;
//            return this;
//        }
//
//        public Builder setHttpClient(HttpClient httpClient) {
//            this.httpClient = httpClient;
//            return this;
//        }
//
//        public Options build(){
//
//            return new Options(environment,endpoint,version,autoConfirm,analytics,
//                    bufferEvents,allocationStore,evolvContext,evolvStore,emitter,httpClient);
//        }
//    }
}
