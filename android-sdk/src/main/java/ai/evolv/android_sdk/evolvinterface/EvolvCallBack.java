package ai.evolv.android_sdk.evolvinterface;

import com.google.gson.JsonObject;

public interface EvolvCallBack<T> {
    void invoke(T t);
}
