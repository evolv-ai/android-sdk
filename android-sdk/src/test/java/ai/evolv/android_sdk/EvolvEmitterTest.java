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

import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_CHANGED;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvolvEmitterTest {

    private static final String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";
    private static final String rawAllocation_one = "{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}";
    private static final String rawRemoteContext_one = "{\"authenticated\":false,\"device\":\"mobile\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}},\"keys\":{\"active\":{\"active_cta_text_165\":\"cta_text_165\",\"active_button_color_165\":\"button_color_165\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}},\"variants\":{\"active\":{\"activeVariants_cta_text_165\":\"cta_text_165:-649973466\",\"activeVariants_button_color_165\":\"button_color_165:112785\",\"activeVariants_home\":\"home:-1951843788\",\"activeVariants_home.cta_text\":\"home.cta_text:1157070792\"}}}";
    private static final String environmentId = "dbcf75051d";
    private static final String uID = "79211876_16178796481581112223332";
    private static final String eID = "81990a9453";
    private static final String cID = "a0e832fc1177:81990a9453";
    private static final String CONFIRMATION_TYPE = "confirmation";
    private static final String CONTAMINATION_TYPE = "contamination";
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
    public void tesTransmit() {

        HttpClient httpClient = spy(OkHttpClient.class);
        EvolvConfig actualConfig = EvolvConfig.builder(environmentId, httpClient) //8b50696b6c
                .build();
        setUpMockedEvolvConfigWithMockedClient(evolvConfig,actualConfig,httpClient);
        EvolvParticipant evolvParticipant = new EvolvParticipant(uID);

        EvolvEmitter evolvEmitter = new EvolvEmitter(evolvConfig,evolvContext,"events",evolvParticipant);
        evolvEmitter.transmit();
    }

}
