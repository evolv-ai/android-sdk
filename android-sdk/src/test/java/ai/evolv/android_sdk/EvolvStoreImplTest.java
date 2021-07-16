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

public class EvolvStoreImplTest {
    //private static final String rawConfiguration = "{\"_published\":1625448126.7474048,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"47d857cd5e\",\"_paused\":false},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"7789bf55d7\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"00436dee0b\",\"_paused\":true}]}";
    private static final String rawExperiment = "{\"web\":{},\"_predicate\":{},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"_paused\":false}";
    private static final String  rawRemoteContext_1 = "{\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";
    private static final String rawConfig_1 = ("{\"_published\":1625881082.2175138,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"47d857cd5e\",\"_paused\":false},{\"web\":{\"dependencies\":\"\",\"o7b7msnxd\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"web.url\",\"operator\":\"regex64_match\",\"value\":\"L2h0dHBzPzpcL1wvW14vXStcL1wvPyg/OiR8XD98IykvaQ==\"}]},\"21zti3xj5\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"hlvg8909z\":{\"_values\":true},\"_initializers\":true}},\"_predicate\":{},\"id\":\"eb18e9a785\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"00436dee0b\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"7789bf55d7\",\"_paused\":true}]}");

    private static String rawGenome_one = "{\"47d857cd5e\":{\"home\":{\"cta_text\":\"Click Here\"},\"next\":{\"layout\":\"Default Layout\"}}}";


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
    public void testGetValueFromRecurse(){

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("home","home.cta_text",
        "next","next.layout"));

        try {
            EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
            JsonElement current = parseRawJsonElement(rawExperiment);
            String parentKey = "";

            evolvStore.recurse(current, parentKey);

            evolvStore.endsWithFilter();

            Assert.assertEquals(evolvStore.getExpLoadedList(), keys);
        } catch (Error e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEvaluatePredicates(){

        JsonArray result_template_one = parseRawJsonElementAsArray("[{\"47d857cd5e_disabled\":{\"rejected_\":\"\"},\"47d857cd5e_entry\":{}},{\"eb18e9a785_disabled\":{\"rejected_web.o7b7msnxd\":\"web.o7b7msnxd\"},\"eb18e9a785_entry\":{}},{\"00436dee0b_disabled\":{\"rejected_\":\"\"},\"00436dee0b_entry\":{}},{\"7789bf55d7_disabled\":{\"rejected_\":\"\"},\"7789bf55d7_entry\":{}}]");

        JsonElement config = parseRawJsonElement(rawConfig_1);
        JsonObject remoteContext = parseRawJsonObject(rawRemoteContext_1);

        EvolvContextImpl evolvContext = new EvolvContextImpl(evolvStore,waitForIt);

        evolvContext.setRemoteContext(remoteContext);

        EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
        JsonArray result_one = evolvStore.evaluatePredicates(1,evolvContext,config);

        Assert.assertEquals(result_template_one, result_one);

    }

    @Test
    public void testGetValue() {

        JsonObject result_template = (parseRawJsonObject("{\"cta_text\":\"Click Here\"}"));

        EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
        JsonObject genome_one = parseRawJsonObject(rawGenome_one);
        evolvStore.setGenomes(genome_one);

        String key_one = "home.cta_text";
        String value_one = evolvStore.getValue(key_one).getAsString();
        Assert.assertEquals(value_one,"Click Here");

        String key_two = "home";
        JsonObject value_two = evolvStore.getValue(key_two).getAsJsonObject();
        Assert.assertEquals(value_two,result_template);

    }

    @Test
    public void testGetActiveKeys() {

        JsonObject active_keys_template = (parseRawJsonObject("{\"active_button_color\":\"button_color\",\"active_cta_text\":\"cta_text\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}"));

        EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
        evolvStore.setActiveKeys(active_keys_template);

        JsonObject activeKeys_result = evolvStore.getActiveKeys();
        Assert.assertEquals(activeKeys_result,active_keys_template);

    }

    @Test
    public void testClearActiveKeys() {

        JsonObject active_keys_template = (parseRawJsonObject("{\"active_button_color\":\"button_color\",\"active_cta_text\":\"cta_text\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}"));
        EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
        evolvStore.setActiveKeys(active_keys_template);
        evolvStore.clearActiveKeys();
        Assert.assertEquals(evolvStore.getActiveKeys().size(),0);

    }

    @Test
    public void testClearActiveKeysPrefix() {

        JsonObject active_keys_template_reuslt = (parseRawJsonObject("{\"active_button_color\":\"button_color\",\"active_cta_text\":\"cta_text\"}"));
        JsonObject active_keys = (parseRawJsonObject("{\"active_button_color\":\"button_color\",\"active_cta_text\":\"cta_text\",\"active_home\":\"home\",\"active_home.cta_text\":\"home.cta_text\"}"));
        EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
        evolvStore.setActiveKeys(active_keys);
        evolvStore.clearActiveKeys("home");
        Assert.assertEquals(active_keys_template_reuslt,evolvStore.getActiveKeys());

    }





}
