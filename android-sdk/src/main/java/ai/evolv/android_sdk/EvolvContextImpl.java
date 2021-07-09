package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.helper.UtilityHelper;

class EvolvContextImpl implements EvolvContext {

    public static String CONTEXT_CHANGED = "context.changed";
    public static String CONTEXT_INITIALIZED = "context.initialized";
    public static String CONTEXT_VALUE_REMOVED = "context.value.removed";
    public static String CONTEXT_VALUE_ADDED = "context.value.added";
    public static String CONTEXT_VALUE_CHANGED = "context.value.changed";
    public static String CONTEXT_DESTROYED = "context.destroyed";

    private String uid;
    private JsonObject remoteContext = new JsonObject();
    private JsonObject localContext = new JsonObject();
    private UtilityHelper helper = new UtilityHelper();
    private boolean initialized = false;
    private EvolvConfig evolvConfig;
    private EvolvStoreImpl evolvStore;
    private WaitForIt waitForIt;

    public EvolvContextImpl(EvolvStoreImpl evolvStore, WaitForIt waitForIt) {
        this.evolvStore = evolvStore;
        this.waitForIt = waitForIt;
    }

    public JsonObject getRemoteContext() {
        return remoteContext;
    }

    @Override
    public void initialize(String uid,
                           JsonObject remoteContext,
                           JsonObject localContext) {

        if (initialized) {
            try {
                throw new EvolvKeyError("Evolv: The context is already initialized");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }

        this.uid = uid;
        if (remoteContext != null) {
            Class genericRemoteContextClass = remoteContext.getClass();
            // TODO: 01.06.2021 redo
            //this.remoteContext = helper.deepCopy(remoteContext, genericRemoteContextClass);
        } else {
            //this.remoteContext = new HashMap<>();
        }
        if (localContext != null) {
            Class genericLocalContextClass = localContext.getClass();
            // TODO: 01.06.2021 redo
            //this.localContext = helper.deepCopy(localContext, genericLocalContextClass);
        } else {
            //this.localContext = new HashMap<>();
        }

        initialized = true;

        JsonElement resolve = resolve();
        List<Object> objects = new ArrayList<>();
        objects.add(CONTEXT_INITIALIZED);
        objects.add(resolve);

        waitForIt.emit(this, CONTEXT_INITIALIZED, objects);
    }


    @Override
    public boolean set(String key, Object value, boolean local) {
        //checking incoming type
        JsonElement jsonValue = null;
        if(value instanceof JsonElement){
            jsonValue = (JsonElement) value;
        }else if(value instanceof String){
            // TODO: use a non-depreciated method "JsonParser"
            JsonParser parser = new JsonParser();
            String modifyValue = ((String)value).replaceAll(" ",".");
            jsonValue = parser.parse(modifyValue);
        }

        ensureInitialized();

        JsonElement context = local ? localContext : remoteContext;
        JsonElement before = helper.getValueForKey(key, context);

        // TODO: 04.06.2021 checking value type (because "before" and "value" need to compare correctly)
        if(before != null) {
            if (before == value || before.toString().equals(value)) {
                return false;
            }
        }

        helper.setKeyToValue(key, jsonValue, context);

        Object updated = this.resolve();

        if (before == null ) {
            // TODO: 04.06.2021 note: I need to decide which type is better to use (JsonElement/List<Object>)
            List<Object> objects = new ArrayList<>(Arrays.asList(
                    CONTEXT_VALUE_ADDED,
                    key,
                    value,
                    local,
                    updated));

            waitForIt.emit(this, CONTEXT_VALUE_ADDED, objects);
        } else {
            // TODO: 04.06.2021 note: I need to decide which type is better to use (JsonElement/List<Object>)
            List<Object> objects = new ArrayList<>(Arrays.asList(
                    CONTEXT_VALUE_CHANGED,
                    key,
                    value,
                    before,
                    local,
                    updated));

            waitForIt.emit(this, CONTEXT_VALUE_CHANGED, objects);
        }

        // TODO: 01.06.2021 understand the data type  emit(,,?)
        waitForIt.emit(this, CONTEXT_CHANGED, updated);
        return true;
    }

    @Override
    public JsonElement resolve() {
        ensureInitialized();
        // TODO: 10.06.2021 implement
        //return objects.deepClone(mutableResolve());
        return new JsonObject();
    }

    private void ensureInitialized() {
        if (!initialized) {
            try {
                throw new EvolvKeyError("Evolv: The evolv context is not initialized");
            } catch (EvolvKeyError evolvKeyError) {
                evolvKeyError.printStackTrace();
            }
        }
    }
}
