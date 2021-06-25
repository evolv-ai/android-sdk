package ai.evolv.android_sdk;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class EvolvPredicatesImplTest {

    private static final String rawExperiment = "{\"web\":{},\"_predicate\":{},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"_paused\":false}";
    private static final String rawContext_one = "{\"key\":{\"test\":\"test_value\"},\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}],\"exclusions\":[]}}";
    private static final String rawContext_two = "{\"signedin\":\"yes\",\"experiments\":{\"allocations\":[{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}],\"exclusions\":[]}}";

    JsonObject parseRawJsonObject(String raw) {
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonObject();
    }

    JsonElement parseRawJsonElement(String raw) {
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonObject();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testValueFromKey() {

        JsonElement context = parseRawJsonObject(rawContext_one);
        EvolvPredicatesImpl predicates = new EvolvPredicatesImpl();

        String key = "key.test";
        JsonElement element = predicates.valueFromKey(context, key);
        Assert.assertEquals(element.getAsString(), "test_value");

        key = "nonExistentKey";
        element = predicates.valueFromKey(context, key);
        Assert.assertEquals(element, JsonNull.INSTANCE);

        key = "experiments.exclusions";
        element = predicates.valueFromKey(context, key);
        Assert.assertEquals(element.getAsJsonArray().size(), 0);

        key = "experiments.allocations";
        element = predicates.valueFromKey(context, key);
        Assert.assertEquals(element.getAsJsonArray().size(), 1);
        Assert.assertEquals(element.getAsJsonArray().get(0).getAsJsonObject().get("uid").getAsString(), "79211876_16178796481581112223331");
        Assert.assertEquals(element.getAsJsonArray().get(0).getAsJsonObject().get("eid").getAsString(), "47d857cd5e");
        Assert.assertEquals(element.getAsJsonArray().get(0).getAsJsonObject().get("cid").getAsString(), "5fa0fd38aae6:47d857cd5e");
        Assert.assertEquals(element.getAsJsonArray().get(0).getAsJsonObject().get("ordinal").getAsString(), "0");
        Assert.assertEquals(element.getAsJsonArray().get(0).getAsJsonObject().get("group_id").getAsString(), "511ce252-92b5-4611-a00c-0e4120369c96");
        Assert.assertFalse(element.getAsJsonArray().get(0).getAsJsonObject().get("excluded").getAsBoolean());

    }

    @Test
    public void testEvaluateFilter() {

        //operator - "equal"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_equal = "{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}";
        //operator - "not_equal"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_not_equal = "{\"field\":\"signedin\",\"operator\":\"not_equal\",\"value\":\"yes\"}";
        //operator - "contains"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_contains = "{\"field\":\"signedin\",\"operator\":\"contains\",\"value\":\"yes\"}";
        //operator - "exists"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_exists = "{\"field\":\"signedin\",\"operator\":\"exists\",\"value\":\"yes\"}";
        //operator - "not_contains"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_not_contains = "{\"field\":\"signedin\",\"operator\":\"not_contains\",\"value\":\"yes\"}";
        //operator - "not_defined"; condition: context - "signedin":"yes", rule - "signedin":"yes"
        String rawRule_not_defined = "{\"field\":\"signedin\",\"operator\":\"not_defined\",\"value\":\"yes\"}";

        JsonElement user = parseRawJsonElement(rawContext_two);
        EvolvPredicatesImpl predicates = new EvolvPredicatesImpl();

        JsonObject rule_equal = parseRawJsonObject(rawRule_equal);
        JsonObject rule_not_equal = parseRawJsonObject(rawRule_not_equal);
        JsonObject rule_contains = parseRawJsonObject(rawRule_contains);
        JsonObject rule_exists = parseRawJsonObject(rawRule_exists);
        JsonObject rule_not_contains = parseRawJsonObject(rawRule_not_contains);
        JsonObject rule_not_defined = parseRawJsonObject(rawRule_not_defined);

        boolean result_equal = predicates.evaluateFilter(user,rule_equal);
        boolean result_not_equal = predicates.evaluateFilter(user,rule_not_equal);
        boolean result_contains = predicates.evaluateFilter(user,rule_contains);
        boolean result_exists = predicates.evaluateFilter(user,rule_exists);
        boolean result_not_contains = predicates.evaluateFilter(user,rule_not_contains);
        boolean result_not_defined = predicates.evaluateFilter(user,rule_not_defined);

        Assert.assertTrue(result_equal);
        Assert.assertFalse(result_not_equal);
        Assert.assertTrue(result_contains);
        Assert.assertTrue(result_exists);
        Assert.assertFalse(result_not_contains);
        Assert.assertFalse(result_not_defined);

    }

    @Test
    public void testEvaluateRule() {

        String rawExperiment_165 = "{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]}";
        String rawRule_165 = "{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"}";

        EvolvPredicatesImpl predicates = new EvolvPredicatesImpl();
        JsonObject passedRules = new JsonObject();
        JsonObject failedRules = new JsonObject();

        JsonElement user = parseRawJsonElement(rawContext_two);
        JsonObject query = parseRawJsonObject(rawExperiment_165);
        JsonObject rule = parseRawJsonObject(rawRule_165);
        boolean passed = predicates.evaluateRule(user,query,rule,passedRules,failedRules);

        Assert.assertFalse(passed);

    }
}
