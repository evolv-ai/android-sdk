package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.Executors;

import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_CHANGED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvolvEmitterTest {

    private final String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";
    private final String rawAllocation_one = "{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}";
    private final String rawRemoteContext_one = "{\"authenticated\":false,\"device\":\"mobile\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{\"active_cta_text_165\":\"cta_text_165\",\"active_button_color_165\":\"button_color_165\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}},\"variants\":{\"active\":{\"activeVariants_cta_text_165\":\"cta_text_165:-649973466\",\"activeVariants_button_color_165\":\"button_color_165:112785\",\"activeVariants_home\":\"home:-1951843788\",\"activeVariants_home.cta_text\":\"home.cta_text:1157070792\"}}}";
    private final String rawMessages_one = "[{\"type\":\"context.initialized\",\"payload\":{\"type\":\"context.initialized\",\"value\":{}},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629975554289},{\"type\":\"context.value.added\",\"payload\":{\"type\":\"context.value.added\",\"key\":\"variants.active\",\"value\":{},\"local\":false,\"updated\":{\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{}},\"variants\":{\"active\":{}}}},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629975604964},{\"type\":\"context.value.added\",\"payload\":{\"type\":\"context.value.added\",\"key\":\"authenticated\",\"value\":false,\"local\":false,\"updated\":{\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{}},\"variants\":{\"active\":{}},\"authenticated\":false}},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629975606225},{\"type\":\"context.value.added\",\"payload\":{\"type\":\"context.value.added\",\"key\":\"device\",\"value\":\"mobile\",\"local\":false,\"updated\":{\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{}},\"variants\":{\"active\":{}},\"authenticated\":false,\"device\":\"mobile\"}},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629975606340}]";
    private final String rawEvents_one = "[{\"type\":\"confirmation\",\"payload\":{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\"},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629987848079}]";
    private final String rawEvents_two = "[{\"type\":\"contamination\",\"payload\":{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"contaminationReason\":{\"reason\":\"error-thrown\",\"details\":\"testing contamination\"}},\"sid\":\"sid_remove_from_server\",\"timestamp\":1629988239817}]";
    private final String environmentId = "dbcf75051d";
    private final String uID = "79211876_16178796481581112223332";
    private final String eID = "81990a9453";
    private final String cID = "a0e832fc1177:81990a9453";
    private final String EVENTS = "events";
    private final String DATA = "data";
    private final String CONFIRMATION_TYPE = "confirmation";
    private final String CONTAMINATION_TYPE = "contamination";
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Mock
    private EvolvConfig evolvConfig;
    @Mock
    private WaitForIt waitForIt;
    @Mock
    private EvolvStoreImpl evolvStore;
    @Mock
    private EvolvClientImpl evolvClient;
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private EvolvConfig mockConfig;
    @Mock
    private EvolvContextImpl evolvContext;


    JsonObject parseRawJsonObject(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    JsonElement parseRawJsonElement(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    JsonArray parseRawJsonElementAsArray(String raw) {
        return JsonParser.parseString(raw).getAsJsonArray();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {

        if (waitForIt != null) {
            waitForIt = null;
        }

        if (evolvConfig != null) {
            evolvConfig = null;
        }

        if (evolvStore != null) {
            evolvStore = null;
        }

        if (evolvClient != null) {
            evolvClient = null;
        }
    }

    EvolvConfig setUpMockedEvolvConfigWithMockedClient(EvolvConfig mockedConfig, EvolvConfig actualConfig
            , HttpClient mockHttpClient) {
        when(mockedConfig.getHttpClient()).thenReturn(mockHttpClient);
        when(mockedConfig.getHttpScheme()).thenReturn(actualConfig.getHttpScheme());
        when(mockedConfig.getDomain()).thenReturn(actualConfig.getDomain());
        when(mockedConfig.getVersion()).thenReturn(actualConfig.getVersion());
        when(mockedConfig.getEndpoint()).thenReturn(actualConfig.getEndpoint());
        when(mockedConfig.getEnvironmentId()).thenReturn(actualConfig.getEnvironmentId());
        when(mockedConfig.getScheduledExecutorService()).thenReturn(Executors.newSingleThreadScheduledExecutor());

        return mockedConfig;
    }

    @Test
    public void testTransmit_data() {
        //sending "data"
        HttpClient httpClient = spy(OkHttpClient.class);
        EvolvConfig actualConfig = EvolvConfig.builder(environmentId, httpClient)
                .build();
        setUpMockedEvolvConfigWithMockedClient(evolvConfig,actualConfig,httpClient);
        EvolvParticipant evolvParticipant = new EvolvParticipant(uID);

        EvolvEmitter evolvEmitter = new EvolvEmitter(evolvConfig,evolvContext,DATA,evolvParticipant);
        evolvEmitter.setMessages(parseRawJsonElementAsArray(rawMessages_one));
        evolvEmitter.transmit();

        //notice: there is a delay of 4000 milliseconds to perform the main calculation functionality (the reason is multithreading)
        try { Thread.sleep(4000); } catch (InterruptedException e) { e.printStackTrace(); }
        verify(httpClient,times(1)).post(eq("https://participants.evolv.ai/v1/dbcf75051d/data"), any());
    }

    @Test
    public void testTransmit_confirmation_events() {
        //sending "events" confirmation
        HttpClient httpClient = spy(OkHttpClient.class);
        EvolvConfig actualConfig = EvolvConfig.builder(environmentId, httpClient)
                .build();
        setUpMockedEvolvConfigWithMockedClient(evolvConfig,actualConfig,httpClient);
        EvolvParticipant evolvParticipant = new EvolvParticipant(uID);

        EvolvEmitter evolvEmitter = new EvolvEmitter(evolvConfig,evolvContext,EVENTS,evolvParticipant);
        evolvEmitter.setMessages(parseRawJsonElementAsArray(rawEvents_one));
        evolvEmitter.transmit();

        verify(httpClient,times(1)).post(eq("https://participants.evolv.ai/v1/dbcf75051d/events"), argThat(new ArgumentMatcher<RequestBody>() {
            @Override
            public boolean matches(Object argument) {
                String body = EvolvClientImplTest.bodyToString((RequestBody) argument);
                JsonObject jsonObject = parseRawJsonObject(body);
                return jsonObject.get("type").getAsString().equals(CONFIRMATION_TYPE);
            }
        }));
    }

    @Test
    public void testTransmit_contamination_events() {
        //sending "events" contamination
        HttpClient httpClient = spy(OkHttpClient.class);
        EvolvConfig actualConfig = EvolvConfig.builder(environmentId, httpClient)
                .build();
        setUpMockedEvolvConfigWithMockedClient(evolvConfig,actualConfig,httpClient);
        EvolvParticipant evolvParticipant = new EvolvParticipant(uID);

        EvolvEmitter evolvEmitter = new EvolvEmitter(evolvConfig,evolvContext,EVENTS,evolvParticipant);
        evolvEmitter.setMessages(parseRawJsonElementAsArray(rawEvents_two));
        evolvEmitter.transmit();

        verify(httpClient,times(1)).post(eq("https://participants.evolv.ai/v1/dbcf75051d/events"), argThat(new ArgumentMatcher<RequestBody>() {
            @Override
            public boolean matches(Object argument) {
                String body = EvolvClientImplTest.bodyToString((RequestBody) argument);
                JsonObject jsonObject = parseRawJsonObject(body);
                return jsonObject.get("type").getAsString().equals(CONTAMINATION_TYPE);
            }
        }));
    }
}
