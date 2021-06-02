package ai.evolv.android_sdk.helper;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
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
}
