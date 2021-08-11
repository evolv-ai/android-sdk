package ai.evolv.android_sdk;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;

import static ai.evolv.android_sdk.EvolvClientImpl.INITIALIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvolvClientImplTest {

    private static String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";
    private static String rawAllocation_one = "{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}";
    private static String rawRemoteContext_one = "{\"authenticated\":false,\"device\":\"mobile\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{\"active_cta_text_165\":\"cta_text_165\",\"active_button_color_165\":\"button_color_165\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}},\"variants\":{\"active\":{\"activeVariants_cta_text_165\":\"cta_text_165:-649973466\",\"activeVariants_button_color_165\":\"button_color_165:112785\",\"activeVariants_home\":\"home:-1951843788\",\"activeVariants_home.cta_text\":\"home.cta_text:1157070792\"}}}";
    private static final String environmentId = "dbcf75051d";
    private static final String userID = "79211876_16178796481581112223332";
    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonObject();
    }

    JsonElement parseRawJsonElement(String raw) {
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonObject();
    }

    JsonArray parseRawJsonElementAsArray(String raw) {
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonArray();
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
        when(mockedConfig.getEnvironmentId()).thenReturn(actualConfig.getEnvironmentId());

        return mockedConfig;
    }

    @Test
    public void testConfirm() {

        HttpClient httpClient = spy(OkHttpClient.class);

        EvolvParticipant evolvParticipant = new EvolvParticipant(userID);
        EvolvConfig config = EvolvConfig.builder(environmentId, httpClient).build();

        EvolvClient client = EvolvClientFactory.init(config, evolvParticipant);
        EvolvContext evolvContext = ((EvolvClientImpl) client).getEvolvContext();

        //remote context
        evolvContext.set("Age", "26", false);
        evolvContext.set("Sex", "female", false);
        evolvContext.set("view", "home", false);

        RequestBody requestBody = RequestBody.create(JSON, "{ " +
                "    \"uid\":\"79211876_16178796481581112\"\n" +
                "    ,\"cid\":\"d73fd69be035:81990a9453\"\n" +
                "    ,\"eid\":\"81990a9453\"\n" +
                "    ,\"type\":\"confirmation\"\n" +
                "    ,\"timestamp\":1628515420511 }");


        client.confirm();

        //notice: there is a delay of 2000 milliseconds to perform the main calculation functionality (the reason is multithreading)
        verify(httpClient, after(2000)).post(eq("https://participants.evolv.ai/v1/dbcf75051d/events"), any());
    }

    private EvolvContextImpl setUpMockedEvolvContext(EvolvContextImpl evolvContext) {

        when(evolvContext.getRemoteContext()).thenReturn(parseRawJsonObject(rawRemoteContext_one));
        return evolvContext;
    }

    @Test
    public void testContaminate() {

        HttpClient httpClient = spy(OkHttpClient.class);

        EvolvParticipant evolvParticipant = new EvolvParticipant(userID);
        EvolvConfig config = EvolvConfig.builder(environmentId, httpClient).build();

        EvolvClient client = EvolvClientFactory.init(config, evolvParticipant);
        EvolvContext evolvContext = ((EvolvClientImpl) client).getEvolvContext();

        //remote context
        evolvContext.set("Age", "26", false);
        evolvContext.set("Sex", "female", false);
        evolvContext.set("view", "home", false);

        RequestBody requestBody = RequestBody.create(JSON, "{ " +
                "    \"uid\":\"79211876_16178796481581112\"\n" +
                "    ,\"cid\":\"d73fd69be035:81990a9453\"\n" +
                "    ,\"eid\":\"81990a9453\"\n" +
                "    ,\"type\":\"contamination\"\n" +
                "    ,\"contaminationReason\": {\"reason\":\"error-thrown\",\"details\":\"testing contamination\"} \n" +
                "    ,\"timestamp\":1628515420511 }");

        JsonObject details = new JsonObject();
        details.addProperty("reason", "error-thrown");
        details.addProperty("details", "testing contamination");

        client.contaminate(details, false);

        //notice: there is a delay of 2000 milliseconds to perform the main calculation functionality (the reason is multithreading)
        verify(httpClient, after(2000)).post(eq("https://participants.evolv.ai/v1/dbcf75051d/events"), any());
    }
}
