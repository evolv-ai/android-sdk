package ai.evolv.android_sdk;

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

import ai.evolv.android_sdk.evolvinterface.EvolvClient;

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
}
