package ai.evolv.android_sdk.helper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.regex.Pattern;

public class UtilityHelper {

    public <T> T deepCopy(T object, Class<T> type) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(object, type), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JsonElement setKeyToValue(String key, JsonElement value, JsonElement map){

        JsonObject current = (JsonObject) map;
        String[] keys = key.split(Pattern.quote("."));
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            if (i == (keys.length - 1)) {
                current.add(k,value);
                break;
            }

            if(!current.has(k)){
                JsonObject jsonObject = new JsonObject();
                current.add(k,jsonObject);
                current = jsonObject;
            }else{
                current = (JsonObject) current.get(k);
            }
        }
        return value;
    }

    public JsonElement getValueForKey(String key, JsonElement map){
        JsonElement value = null;
        JsonObject current = (JsonObject) map;
        String[] keys = key.split(Pattern.quote("."));
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            if (i == (keys.length - 1)) {
                value = current.get(k);
                break;
            }

            if(!current.has(k)){
                break;
            }
                current = (JsonObject) current.get(k);

        }
        return value;
    }

    public JsonObject filter(JsonObject map, JsonObject active) {
        JsonElement pruned = prune(map, active);
        return expand(pruned);
    }

    public JsonElement prune(JsonObject map, JsonObject active) {

        JsonObject pruned = new JsonObject();

        for (Map.Entry<String, JsonElement> key : active.entrySet()) {

            String[] keyParts = key.getValue().getAsString().split(Pattern.quote("."));

            JsonElement current = map;
            for (int i = 0; i < keyParts.length; i++) {

                JsonElement now = null;
                if (!current.getAsJsonObject().has(keyParts[i])){
                    continue;
                }
                if(current.getAsJsonObject().get(keyParts[i]).isJsonObject()){
                    now = current.getAsJsonObject().get(keyParts[i]).getAsJsonObject();
                }else if(current.getAsJsonObject().get(keyParts[i]).isJsonPrimitive()){
                    now = current.getAsJsonObject().get(keyParts[i]).getAsJsonPrimitive();
                }

                if (!now.isJsonNull()) {
                    if (i == keyParts.length - 1) {
                        pruned.add(key.getValue().getAsString(), now);
                        break;
                    }
                    current = now;
                } else {
                    break;
                }
            }
        }

        // TODO: 05.07.2021 add
        //reattributePredicatedValues(pruned, active);
        return pruned;
    }


    private JsonObject expand(JsonElement map) {

        JsonObject expanded = new JsonObject();

        for (String key : map.getAsJsonObject().keySet()) {

            JsonElement v = null;
            if(map.getAsJsonObject().get(key).isJsonObject()){
                v = map.getAsJsonObject().get(key).getAsJsonObject();
            }else if(map.getAsJsonObject().get(key).isJsonPrimitive()){
                v = map.getAsJsonObject().get(key).getAsJsonPrimitive();
            }
            setKeyToValue(key,v,expanded);
        }

        return expanded;
    }

}
