package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class EvolvPredicatesImpl {

    //private JsonArray result = new JsonArray();
    private JsonObject result = new JsonObject();

    // TODO: 11.06.2021 need a unit test
    public JsonElement evaluate(JsonElement context, JsonElement predicate) {

        JsonObject passed = new JsonObject();
        JsonObject failed = new JsonObject();
        JsonObject touched = new JsonObject();
        JsonObject rejected = new JsonObject();
        // TODO: 11.06.2021 define the type for the result
        result.add("passed",passed);
        result.add("failed",failed);
        result.add("touched",touched);
        result.add("rejected",rejected);

        JsonObject predicateObject = null;

        if (predicate.isJsonObject()) {
            predicateObject = predicate.getAsJsonObject();
        }

        rejected.addProperty("rejected", !evaluatePredicate(context, predicateObject, passed, failed));

        // TODO: 10.06.2021 to figure out what the type can be for "result"  -> result.passed

        Iterator<JsonElement> iterator = result.iterator();

        while(iterator.hasNext()){
            JsonObject jsonObject = iterator.next().getAsJsonObject();

            if(jsonObject.has("passed")){
                if(result.has("touched")){
                    result.get("touched").add(jsonObject.get("field"));
                }
            }
            if(jsonObject.has("failed")){
                if(result.has("touched")){
                    result.get("touched").add(jsonObject.get("field"));
                }
            }
        }

        return result;
    }

    // TODO: 11.06.2021 need a unit test
    private boolean evaluatePredicate(JsonElement user,
                                      JsonObject query,
                                      JsonObject passedRules,
                                      JsonObject failedRules) {

        JsonArray rules = null;
        if (!query.has("rules")) {
            //rules = query.get("rules").getAsJsonObject();
            return true;
        }

        if (rules.isJsonNull() || rules.size() == 0) {
            return true;
        }

        String combinator = query.get("combinator").getAsString();
        for (int i = 0; i < rules.size(); i++) {
            boolean passed = evaluateRule(user, query, rules.get(i).getAsJsonObject(), passedRules, failedRules);

            if (passed && combinator.equals("OR")) {
                return true;
            }
            if (passed && combinator.equals("AND")) {
                return false;
            }
        }

        // If we've reached this point on an 'or' all rules failed.
        return combinator.equals("AND");
    }

    // TODO: 11.06.2021 need a unit test
    private boolean evaluateRule(JsonElement user,
                                 JsonObject query,
                                 JsonObject rule,
                                 JsonObject passedRules,
                                 JsonObject failedRules) {
        
        boolean result;

        if(rule.has("combinator")){
            // No need to add groups to pass/failed rule sets here. Their children results will be merged up
            // via recursion.
            // eslint-disable-next-line no-use-before-define
            return evaluatePredicate(user, rule, passedRules, failedRules);
        }else{
            result = evaluateFilter(user, rule);
        }

        // Any other rule is also a terminating branch in our recursion tree, so we add rule id to pass/fail rule set
        if(result) {
            passedRules.addProperty("id",query.get("id").getAsString());
            passedRules.addProperty("index",rule.get("index").getAsString());
            passedRules.addProperty("field",rule.get("field").getAsString());

        }else{
            failedRules.addProperty("id",query.get("id").getAsString());
            failedRules.addProperty("index",rule.get("index").getAsString());
            failedRules.addProperty("field",rule.get("field").getAsString());
        }

        return result;
    }

    // TODO: 11.06.2021 need a unit test
    private boolean evaluateFilter(JsonElement user, JsonObject rule) {
        JsonElement value = valueFromKey(user, rule.get("field").getAsJsonObject());

        if (rule.get("operator").getAsString().startsWith("kv_") && value.isJsonNull() ) {
            return false;
        }

        // TODO: 11.06.2021 implement filter
         //Evaluates a single filter rule against a user.
        //return !!FILTER_OPERATORS[rule.operator](value, rule.value);
        return false;

    }

    private JsonElement valueFromKey(JsonElement user, JsonObject rule) {
        // TODO: 10.06.2021
        return null;
    }


}
