package ai.evolv.android_sdk;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;

public class EvolvClientImplTest {

    private static String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";

    @Mock
    private EvolvConfig evolvConfig;
    @Mock
    private WaitForIt waitForIt;
    @Mock
    private EvolvStoreImpl evolvStore;
    @Mock
    private EvolvClientImpl evolvClient;
    @Mock
    private Log log;


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

    @Test
    public void testConfirm() {

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 7000);
        EvolvConfig config = EvolvConfig.builder("dbcf75051d", httpClient).build();
        EvolvClient client = EvolvClientFactory.init(config, new EvolvParticipant("79211876_16178796481581112223332"));

        EvolvContext evolvContext = ((EvolvClientImpl) client).getEvolvContext();

        evolvContext.set("Age", "26", false);
        evolvContext.set("Sex", "female", false);
        evolvContext.set("view", "home", false);

        client.confirm();


    }

    @Test
    public void testContaminate() {

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 7000);
        EvolvConfig config = EvolvConfig.builder("dbcf75051d", httpClient).build();
        EvolvClient client = EvolvClientFactory.init(config, new EvolvParticipant("79211876_16178796481581112223332"));

        EvolvContext evolvContext = ((EvolvClientImpl) client).getEvolvContext();

        evolvContext.set("Age", "26", false);
        evolvContext.set("Sex", "female", false);
        evolvContext.set("view", "home", false);

        JsonObject details = new JsonObject();
        details.addProperty("reason","error-thrown");
        details.addProperty("details","testing contamination");

        client.contaminate(details,false);
    }

}
