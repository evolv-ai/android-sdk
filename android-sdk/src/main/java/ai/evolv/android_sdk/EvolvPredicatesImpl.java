package ai.evolv.android_sdk;

import android.os.Build;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class EvolvPredicatesImpl {

    private JsonObject result = new JsonObject();

    public JsonElement evaluate(JsonElement context, JsonElement predicate) {

        JsonObject passed = new JsonObject();
        JsonObject failed = new JsonObject();
        JsonObject touched = new JsonObject();
        JsonObject rejected = new JsonObject();

        result.add("passed", passed);
        result.add("failed", failed);
        result.add("touched", touched);
        result.add("rejected", rejected);

        JsonObject predicateObject = null;

        if (predicate.isJsonObject()) {
            predicateObject = predicate.getAsJsonObject();
        }
        boolean rejectedPredicate = !evaluatePredicate(context, predicateObject, passed, failed);
        rejected.addProperty("rejected", rejectedPredicate);

        JsonObject passedObject = result.get("passed").getAsJsonObject();
        JsonObject failedObject = result.get("failed").getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : passedObject.entrySet()) {
            result.get("touched").getAsJsonObject().add(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, JsonElement> entry : failedObject.entrySet()) {
            result.get("touched").getAsJsonObject().add(entry.getKey(), entry.getValue());
        }

        return result;
    }

    boolean evaluatePredicate(JsonElement user,
                              JsonObject query,
                              JsonObject passedRules,
                              JsonObject failedRules) {

        JsonArray rules = new JsonArray();

        if (query.has("rules")) {
            rules = query.get("rules").getAsJsonArray();
        } else {
            return true;
        }

        if (rules.isJsonNull() || rules.size() == 0) {
            return true;
        }

        String combinator = query.get("combinator").getAsString();
        for (int i = 0; i < rules.size(); i++) {
            boolean passed = evaluateRule(user, query, rules.get(i).getAsJsonObject(), passedRules, failedRules);

            if (passed && combinator.equals("or")) {
                return true;
            }
            if (!passed && combinator.equals("and")) {
                return false;
            }
        }

        return combinator.equals("and");
    }

    boolean evaluateRule(JsonElement user,
                         JsonObject query,
                         JsonObject rule,
                         JsonObject passedRules,
                         JsonObject failedRules) {

        boolean result;

        if (rule.has("combinator")) {
            // No need to add groups to pass/failed rule sets here. Their children results will be merged up
            // via recursion.
            // eslint-disable-next-line no-use-before-define
            return evaluatePredicate(user, rule, passedRules, failedRules);
        } else {
            result = evaluateFilter(user, rule);
        }

        // Any other rule is also a terminating branch in our recursion tree, so we add rule id to pass/fail rule set
        if (result) {
            if (rule.has("id")) passedRules.addProperty("id", query.get("id").getAsString());
            if (rule.has("index"))
                passedRules.addProperty("index", rule.get("index").getAsString());
            if (rule.has("field"))
                passedRules.addProperty("field", rule.get("field").getAsString());

        } else {
            if (rule.has("id")) failedRules.addProperty("id", query.get("id").getAsString());
            if (rule.has("index"))
                failedRules.addProperty("index", rule.get("index").getAsString());
            if (rule.has("field"))
                failedRules.addProperty("field", rule.get("field").getAsString());
        }

        return result;
    }

    boolean evaluateFilter(JsonElement user, JsonObject rule) {
        JsonElement value = valueFromKey(user, rule.get("field").getAsString());

        if (value.isJsonNull()) {
            return false;
        }

        //Evaluates a single filter rule against a user.
        return operators.get(rule.get("operator").getAsString()).apply(value.getAsString(), rule.get("value").getAsString());
    }

    JsonElement valueFromKey(JsonElement context, String key) {
        if (context == null || context.isJsonNull()){
            return JsonNull.INSTANCE;
        }
        int nextToken = key.indexOf('.');

        if (nextToken == 0) {
            throw new Error("Invalid variant key: " + key);
        }

        if (nextToken == -1) {
            if (context.getAsJsonObject().has(key)) {
                return context.getAsJsonObject().get(key);
            } else {
                return JsonNull.INSTANCE;
            }
        }
        return valueFromKey(context.getAsJsonObject().get(key.substring(0, nextToken)), key.substring(nextToken + 1));
    }

    @FunctionalInterface
    interface Function<A, B> {
        boolean apply(A one, B two);
    }

    private final Map<String, Function> operators = createOperatorsMap();

    private Map<String, Function> createOperatorsMap() {
        Map<String, Function> operatorsMap = new HashMap<>();

        operatorsMap.put("contains", (Function<String, String>) String::contains);
        operatorsMap.put("defined", (Function<String, String>) (a, b) -> a != null);
        operatorsMap.put("equal", (Function<String, String>) String::equals);
        operatorsMap.put("exists", (Function<String, String>) (a, b) -> a != null);
        operatorsMap.put("not_contains", (Function<String, String>) (a, b) -> !(a.contains(b)));
        operatorsMap.put("not_defined", (Function<String, String>) (a, b) -> a == null);
        operatorsMap.put("not_equal", (Function<String, String>) (a, b) -> !a.equals(b));
        operatorsMap.put("not_regex_match", (Function<String, String>) (a, b) -> !Pattern.compile(b).matcher(a).matches());
        operatorsMap.put("not_regex64_match", (Function<String, String>) (value, b64pattern) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                byte[] decode = Base64.getDecoder().decode(b64pattern);
                String decodeString = new String(decode);
                return  !Pattern.compile(value).matcher(decodeString).matches();
            }
            return false;
        });
        operatorsMap.put("not_starts_with", (Function<String, String>) (a, b) -> !a.startsWith(b));
        operatorsMap.put("starts_with", (Function<String, String>) String::startsWith);
        operatorsMap.put("regex_match", (Function<String, String>) (a, b) -> Pattern.compile(b).matcher(a).matches());
        operatorsMap.put("regex64_match", (Function<String, String>) (value, b64pattern) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                byte[] decode = Base64.getDecoder().decode(b64pattern);
                String decodeString = new String(decode);
                return  Pattern.compile(value).matcher(decodeString).matches();
            }
            return false;
        });

        return operatorsMap;
    }
}
