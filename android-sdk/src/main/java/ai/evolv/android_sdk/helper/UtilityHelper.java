package ai.evolv.android_sdk.helper;

import com.google.gson.Gson;

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
}
