package ai.evolv.android_sdk;

import com.google.gson.JsonArray;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;

public class DefaultAllocationStore implements EvolvAllocationStore {

    private LruCache cache;

    DefaultAllocationStore(int size) {
        this.cache = new LruCache(size);
    }

    @Override
    public JsonArray get(String uid) {
        return cache.getEntry(uid);
    }

    @Override
    public void put(String uid, JsonArray allocations) {
        cache.putEntry(uid, allocations);
    }
}
