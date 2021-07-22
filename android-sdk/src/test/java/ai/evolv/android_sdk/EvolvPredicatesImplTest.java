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
    private static final String rawContext_four = "{\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";

    //testEvaluatePredicate
    private static final String rawUser_one = "{\"signedin\":\"yes\",\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";
    private static final String rawUser_two = "{\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";
    private static final String rawQuery_one = "{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]}";
    private static final String rawQuery_two = "{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]}";

    //testEvaluate
    private static final String rawContext_three = "{\"Age\":26,\"Sex\":\"female\",\"view\":\"home\",\"experiments\":{\"allocations\":{\"allocs\":{\"uid\":\"79211876_16178796481581112223331\",\"eid\":\"47d857cd5e\",\"cid\":\"5fa0fd38aae6:47d857cd5e\",\"ordinal\":0,\"group_id\":\"511ce252-92b5-4611-a00c-0e4120369c96\",\"excluded\":false}},\"exclusions\":{}}}";
    private static final String rawPredicate_one = "{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]}";
    private static final String rawPredicate_two = "{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]}";
    private static final String rawPredicate_three = "{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]}";
    private static final String rawPredicate_four = "{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]}";
    private static final String rawPredicate_five = "{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]}";


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
        JsonElement context_four = parseRawJsonObject(rawContext_four);

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


        JsonElement jsonElement = predicates.valueFromKey(context_four, "Age");
        Assert.assertEquals(jsonElement.getAsString(), "26");

        jsonElement = predicates.valueFromKey(context_four,"view" );
        Assert.assertEquals(jsonElement.getAsString(), "home");

        jsonElement = predicates.valueFromKey(context_four, "Sex");
        Assert.assertEquals(jsonElement.getAsString(), "female");

        jsonElement = predicates.valueFromKey(context_four,"experiments.allocations");
        Assert.assertEquals(jsonElement.getAsJsonObject().size(), 1);
        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().size(), 6);

        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("uid").getAsString(), "79211876_16178796481581112223331");
        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("eid").getAsString(), "47d857cd5e");
        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("cid").getAsString(), "5fa0fd38aae6:47d857cd5e");
        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("ordinal").getAsString(), "0");
        Assert.assertEquals(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("group_id").getAsString(), "511ce252-92b5-4611-a00c-0e4120369c96");
        Assert.assertFalse(jsonElement.getAsJsonObject().get("allocs").getAsJsonObject().get("excluded").getAsBoolean());

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

    @Test
    public void testEvaluatePredicate() {

        EvolvPredicatesImpl predicates = new EvolvPredicatesImpl();

        JsonElement user_one = parseRawJsonElement(rawUser_one);
        JsonElement user_two = parseRawJsonElement(rawUser_two);
        JsonObject query_one = parseRawJsonObject(rawQuery_one);
        JsonObject query_two = parseRawJsonObject(rawQuery_two);

        JsonObject passedRules = new JsonObject();
        JsonObject failedRules = new JsonObject();

        boolean result_one = predicates.evaluatePredicate(user_one,query_one,passedRules,failedRules);
        boolean result_two = predicates.evaluatePredicate(user_two,query_two,passedRules,failedRules);

        Assert.assertTrue(result_one);
        Assert.assertFalse(result_two);
    }

    @Test
    public void testEvaluate() {

        JsonElement result_template_one = parseRawJsonElement("{\"passed\":{\"field\":\"Sex\"},\"failed\":{},\"touched\":{\"field\":\"Sex\"},\"rejected\":{\"rejected\":false}}");
        JsonElement result_template_two = parseRawJsonElement("{\"passed\":{\"field\":\"view\"},\"failed\":{},\"touched\":{\"field\":\"view\"},\"rejected\":{\"rejected\":false}}");
        JsonElement result_template_three = parseRawJsonElement("{\"passed\":{},\"failed\":{\"field\":\"view\"},\"touched\":{\"field\":\"view\"},\"rejected\":{\"rejected\":true}}");
        JsonElement result_template_four = parseRawJsonElement("{\"passed\":{},\"failed\":{\"field\":\"signedin\"},\"touched\":{\"field\":\"signedin\"},\"rejected\":{\"rejected\":true}}");
        JsonElement result_template_five = parseRawJsonElement("{\"passed\":{},\"failed\":{\"field\":\"authenticated\"},\"touched\":{\"field\":\"authenticated\"},\"rejected\":{\"rejected\":true}}");

        EvolvPredicatesImpl predicates = new EvolvPredicatesImpl();

        JsonElement context = parseRawJsonElement(rawContext_three);

        JsonElement predicate_one = parseRawJsonElement(rawPredicate_one);
        JsonElement predicate_two = parseRawJsonElement(rawPredicate_two);
        JsonElement predicate_three = parseRawJsonElement(rawPredicate_three);
        JsonElement predicate_four = parseRawJsonElement(rawPredicate_four);
        JsonElement predicate_five = parseRawJsonElement(rawPredicate_five);

        JsonElement result_one = predicates.evaluate(context,predicate_one);
        Assert.assertEquals(result_template_one, result_one);

        JsonElement result_two = predicates.evaluate(context,predicate_two);
        Assert.assertEquals(result_template_two, result_two);

        JsonElement result_three = predicates.evaluate(context,predicate_three);
        Assert.assertEquals(result_template_three, result_three);

        JsonElement result_four = predicates.evaluate(context,predicate_four);
        Assert.assertEquals(result_template_four, result_four);

        JsonElement result_five = predicates.evaluate(context,predicate_five);
        Assert.assertEquals(result_template_five, result_five);
    }
}
