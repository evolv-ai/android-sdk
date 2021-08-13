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
import java.util.Arrays;

public class EvolvContextImplTest {
    //private static final String rawConfiguration = "{\"_published\":1625448126.7474048,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"47d857cd5e\",\"_paused\":false},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"7789bf55d7\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"00436dee0b\",\"_paused\":true}]}";
    private static final String rawExperiment_one = "{\"web\":{},\"_predicate\":{},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"_paused\":false}";
    private static final String rawExperiment_two = "{\"5d0d177a37\":{\"loaded\":{\"loaded_keys\":[\"cta_text_165\",\"button_color_165\"]},\"active\":{\"active_cta_text_165\":\"cta_text_165\",\"active_button_color_165\":\"button_color_165\"}},\"81990a9453\":{\"loaded\":{\"loaded_keys\":[\"home\",\"home.cta_text\",\"next\",\"next.layout\"]}},\"9c83cf9cde\":{\"loaded\":{\"loaded_keys\":[\"button_color\",\"cta_text\"]}}}";
    private static final String rawRemoteContext_one = "{\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";
    private static final String rawRemoteContext_two = "{\"authenticated\":false,\"device\":\"mobile\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}}}";
    private static final String rawRemoteContext_three = "{\"authenticated\":false,\"device\":\"mobile\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}}}";
    private static final String rawRemoteContext_four = "{\"signedin\":\"yes\",\"authenticated\":false,\"device\":\"mobile\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}],\"exclusions\":{}}}";
    private static final String rawConfig_one = ("{\"_published\":1625881082.2175138,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"47d857cd5e\",\"_paused\":false},{\"web\":{\"dependencies\":\"\",\"o7b7msnxd\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"web.url\",\"operator\":\"regex64_match\",\"value\":\"L2h0dHBzPzpcL1wvW14vXStcL1wvPyg/OiR8XD98IykvaQ==\"}]},\"21zti3xj5\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"hlvg8909z\":{\"_values\":true},\"_initializers\":true}},\"_predicate\":{},\"id\":\"eb18e9a785\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"00436dee0b\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"7789bf55d7\",\"_paused\":true}]}");
    private static final String rawConfig_two = "{\"_published\":1628102785.995071,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"}]},\"cta_text_165\":{\"_is_entry_point\":true,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"button_color_165\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"id\":\"5d0d177a37\",\"_paused\":false},{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"81990a9453\",\"_paused\":false},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"contains\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":true,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":true,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"9c83cf9cde\",\"_paused\":true}]}";
    private static String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";

    private static String rawAllocations_one = "[{\"uid\":\"79211876_16178796481581112\",\"eid\":\"5d0d177a37\",\"cid\":\"4a10ac5a13bf:5d0d177a37\",\"genome\":{\"cta_text_165\":\"Add to Cart\",\"button_color_165\":\"red\"},\"audience_query\":{\"id\":165,\"name\":\"Sign out\",\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"}]},\"ordinal\":1,\"group_id\":\"ec5e31c4-9e7e-44c9-b815-7aaf3f8f8ffe\",\"excluded\":false},{\"uid\":\"79211876_16178796481581112\",\"eid\":\"81990a9453\",\"cid\":\"d73fd69be035:81990a9453\",\"genome\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Layout 1\"}},\"audience_query\":{\"id\":174,\"name\":\"Test_Audiences\",\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"ordinal\":1,\"group_id\":\"e43d91d5-54cf-4957-aad8-afd55a86932d\",\"excluded\":false}]";

    private int version = 1;

    @Mock
    private EvolvParticipant participant;
    @Mock
    private WaitForIt waitForIt;
    @Mock
    private EvolvConfig evolvConfig;
    @Mock
    private EvolvStoreImpl evolvStore;

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
        if (participant != null) {
            participant = null;
        }

        if (waitForIt != null) {
            waitForIt = null;
        }

        if (evolvConfig != null) {
            evolvConfig = null;
        }

        if (evolvStore != null) {
            evolvStore = null;
        }
    }

    @Test
    public void setTest() {

        EvolvContextImpl evolvContext = new EvolvContextImpl(evolvStore,waitForIt);
        evolvContext.setInitialized(true);
        //case 1
        boolean result_one = evolvContext.set("signedin", "yes", false);
        Assert.assertTrue(result_one);
        //case 2
        boolean result_two = evolvContext.set("signedin", "no", false);
        Assert.assertTrue(result_two);
        //case 3
        boolean result_three = evolvContext.set("testkey.test", "value", false);
        Assert.assertTrue(result_three);
        //case 4
        boolean result_four = evolvContext.set("myKey", "testValue", false);
        Assert.assertTrue(result_four);
        //case 5
        boolean result_five = evolvContext.set("myKey", "testValue", false);
        Assert.assertFalse(result_five);
    }

}
