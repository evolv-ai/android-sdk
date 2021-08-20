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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_ADDED;

public class WaitForItTest {

    private static String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";

    @Mock
    private EvolvConfig evolvConfig;
    @Mock
    private EvolvStoreImpl evolvStore;
    @Mock
    private EvolvContextImpl evolvContext;

    private WaitForIt waitForIt = new WaitForIt();



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

        if (evolvConfig != null) {
            evolvConfig = null;
        }

        if (evolvStore != null) {
            evolvStore = null;
        }

        if (evolvContext != null) {
            evolvContext = null;
        }
    }

    @Test
    public void testWaitFor() {
        waitForIt.waitFor(evolvContext, CONTEXT_VALUE_ADDED, (EvolvInvocation<JsonObject>) type -> {
            if (type.has("local")) {
                if (type.get("local").getAsBoolean()) {
                    return;
                }
            }
            JsonObject payloadMap = type;
            Assert.assertTrue(true);
        });
    }

    @Test
    public void testEmit(){
        String it = CONTEXT_VALUE_ADDED;
        String payloadList = "{\"type\":\"context.value.added\",\"key\":\"authenticated\",\"value\":false,\"local\":false,\"updated\":{}}";
        JsonObject jsonObject = parseRawJsonObject(payloadList);

        //in order for the call of the handler.invoke(payload), we must fill with the correct condition in the  handlers.put(it, list) waitFor method
        testWaitFor();
        waitForIt.emit(evolvContext,it,jsonObject);
    }

}
