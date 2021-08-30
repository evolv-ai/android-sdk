package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.helper.UtilityHelper;

import static ai.evolv.android_sdk.EvolvStoreImpl.EMPTY_STRING;

public class EvolvContextImpl implements EvolvContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvContextImpl.class);
    private static final int DEFAULT_QUEUE_LIMIT = 50;

    public static String CONTEXT_CHANGED = "context.changed";
    public static String CONTEXT_INITIALIZED = "context.initialized";
    public static String CONTEXT_VALUE_REMOVED = "context.value.removed";
    public static String CONTEXT_VALUE_ADDED = "context.value.added";
    public static String CONTEXT_VALUE_CHANGED = "context.value.changed";
    public static String CONTEXT_DESTROYED = "context.destroyed";

    private String uid;
    private JsonObject remoteContext = new JsonObject();
    private JsonObject localContext = new JsonObject();
    private final UtilityHelper helper = new UtilityHelper();
    private boolean initialized = false;
    private EvolvConfig evolvConfig;
    private EvolvStoreImpl evolvStore;
    private WaitForIt waitForIt;
    private JsonObject flattened = new JsonObject();

    public EvolvContextImpl(EvolvStoreImpl evolvStore, WaitForIt waitForIt) {
        this.evolvStore = evolvStore;
        this.waitForIt = waitForIt;
    }

    public JsonObject getRemoteContext() {
        return remoteContext;
    }

    public void setRemoteContext(JsonObject remoteContext) {
        this.remoteContext = remoteContext;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
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

        this.remoteContext = remoteContext != null ? remoteContext.deepCopy() : new JsonObject();
        this.localContext = localContext != null ? localContext.deepCopy() : new JsonObject();

        initialized = true;

        JsonElement resolve = resolve();
        JsonObject object = new JsonObject();
        object.addProperty("type", CONTEXT_INITIALIZED);
        object.add("value", resolve);
        
        waitForIt.emit(this, CONTEXT_INITIALIZED, object);
    }


    @Override
    public boolean set(String key, Object value, boolean local) {

        JsonElement jsonValue = null;
        if (value instanceof JsonElement) {
            jsonValue = (JsonElement) value;
        } else if (value instanceof String) {

            String modifyValue = ((String) value).replaceAll(" ", ".");
            jsonValue = JsonParser.parseString(modifyValue);
        }

        ensureInitialized();

        JsonElement context = local ? localContext : remoteContext;
        JsonElement before = helper.getValueForKey(key, context);

        if (before != null) {
            if(before.isJsonPrimitive()){
                if (before.getAsString().equals(value) || before.toString().equals(value)) {
                    return false;
                }
            }else return false;
        }

        helper.setKeyToValue(key, jsonValue, context);

        JsonObject updated = resolve();

        if (before == null) {

            JsonObject objects = new JsonObject();
            objects.addProperty("type", CONTEXT_VALUE_ADDED);
            objects.addProperty("key", key);
            objects.add("value", jsonValue);
            objects.addProperty("local", false);
            objects.add("updated", updated);


            waitForIt.emit(this, CONTEXT_VALUE_ADDED, objects);
        } else {

            JsonObject objects = new JsonObject();
            objects.addProperty("type", CONTEXT_VALUE_CHANGED);
            objects.addProperty("key", key);
            objects.add("value", jsonValue);
            objects.add("before", before);
            objects.addProperty("local", false);
            objects.add("updated", updated);

            waitForIt.emit(this, CONTEXT_VALUE_CHANGED, objects);
        }

        waitForIt.emit(this, CONTEXT_CHANGED, updated);
        return true;

    }

    @Override
    public JsonObject resolve() {
        ensureInitialized();
        JsonObject jsonLocalObject = localContext.deepCopy();
        JsonObject jsonRemoteObject = remoteContext.deepCopy();

        JsonObject mergeResult = deepMerge(jsonRemoteObject,jsonLocalObject);

        return mergeResult;
    }

    private JsonObject deepMerge(JsonObject source, JsonObject target) throws JsonIOException {
        for (String key: source.keySet()) {
            JsonElement value = source.get(key);
            if (!target.has(key)) {
                if(value.isJsonPrimitive()){
                    target.addProperty(key, value.getAsString());
                }else{
                    target.add(key, value);
                }
            } else {
                if (value.isJsonObject()) {
                    JsonObject valueJson = value.getAsJsonObject();
                    deepMerge(valueJson, target.get(key).getAsJsonObject());
                } else {
                    target.addProperty(key, value.getAsString());
                }
            }
        }
        return target;
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

    @Override
    public JsonElement get(String key) {
        ensureInitialized();

        JsonElement element = helper.getValueForKey(key, remoteContext);

        if (element == null || element.isJsonNull()) {
            return helper.getValueForKey(key, localContext).getAsJsonObject();
        } else {
            return element;
        }
    }

    @Override
    public boolean pushToArray(String key, String value, boolean local) {
        int limit = DEFAULT_QUEUE_LIMIT;
        return pushToArray(key,value,local,limit);
    }

    @Override
    public boolean pushToArray(String key, String value, boolean local, int limit) {
        ensureInitialized();
        JsonObject context = local ? localContext : remoteContext;

        JsonArray newArray = new JsonArray();
        JsonObject element = new JsonObject();

        JsonElement originalArray = helper.getValueForKey(key, context);

        if (originalArray == null) {
            LOGGER.error("The " + "\"" + key + "\"" + " does not have a suitable value ");
            return false;
        } else if (originalArray.isJsonPrimitive()) {
            element.addProperty(key, value);
        } else if (originalArray.isJsonObject()) {
            element.add(key, originalArray);
        }

        newArray.add(element);

        for (int i = 0; i < newArray.size(); i++) {
            if (i > limit) {
                newArray.remove(i);
            }
        }
        return set(key, newArray, local);
    }

    @Override
    public boolean contains(String key) {
        ensureInitialized();
        return remoteContext.has(key) || localContext.has(key);
    }

    @Override
    public void update(JsonObject update, boolean local) {
        ensureInitialized();
        JsonObject context = local ? localContext : remoteContext;

        String incomingKey  = "";
        JsonArray incomingArray  = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : update.entrySet()) {
            incomingKey = entry.getKey();
            incomingArray = entry.getValue().getAsJsonArray();
        }

        flatten(update);

        JsonObject flattenedBefore = new JsonObject();

        for (String key : flattened.keySet()) {
            flattenedBefore.add(key,context.get(key));
        }

        if (local) {
            set(incomingKey,incomingArray,true);
            context = localContext;
        } else {
            set(incomingKey,incomingArray,false);
            context = remoteContext;
        }

        JsonObject updated = this.resolve();

        for (String key : flattened.keySet()) {

            if (flattenedBefore.get(key) == null) {

                JsonObject object = new JsonObject();
                object.addProperty("type",CONTEXT_VALUE_ADDED);
                object.addProperty("key",key);
                object.add("flattened_key",flattened.get(key));
                object.addProperty("local",local);
                object.add("updated",updated);

                waitForIt.emit(this, CONTEXT_VALUE_ADDED, object);
            }else if (flattenedBefore.get(key) != context.get(key)) {

                JsonObject object = new JsonObject();
                object.addProperty("type",CONTEXT_VALUE_CHANGED);
                object.addProperty("key",key);
                object.add("flattened_key",flattened.get(key));
                object.add("flattenedBefore_key",flattenedBefore.get(key));
                object.addProperty("local",local);
                object.add("updated",updated);

                waitForIt.emit(this, CONTEXT_VALUE_CHANGED, object);
            }
        }
    }

    private void flatten(JsonElement map) {
        recurseFlatten(map, "");
    }

    private String recurseFlatten(JsonElement current, String parentKey) {
        Set<String> keys = current.getAsJsonObject().keySet();

        Iterator<String> iterator = keys.iterator();
        Set<String> items = new HashSet<>();

        while (iterator.hasNext()) {
            String key = iterator.next();

            if (startsWithFilter.apply(key)) {
                JsonElement element = current.getAsJsonObject().get(key);
                if (element.isJsonObject()) {
                    if (element.getAsJsonObject().size() == 0) continue;
                }
                String newKey = !parentKey.isEmpty() ? (parentKey + '.' + key) : key;
                items.add(newKey);
                flattened.addProperty(newKey,newKey);

                if (element.isJsonObject()) {
                    if (element.getAsJsonObject().size() != 0) {
                        element = current.getAsJsonObject().get(key);
                        items.add(key.concat(recurseFlatten(element, newKey)));
                    }
                }
            }
        }
        return EMPTY_STRING;
    }

    EvolvStoreImpl.Filter<String> startsWithFilter = key -> !key.startsWith("_")
            || key.equals("_values")
            || key.equals("_initializers");

    @Override
    public boolean remove(String key) {
        ensureInitialized();

        boolean local = removeValueForKey(key, localContext);
        boolean remote = removeValueForKey(key, remoteContext);

        boolean removed = local || remote;

        if (removed) {
            JsonObject updated = this.resolve();

            JsonObject objectValueRemoved = new JsonObject();
            objectValueRemoved.addProperty("type", CONTEXT_VALUE_REMOVED);
            objectValueRemoved.addProperty("key", key);
            objectValueRemoved.addProperty("remote", !remote);
            objectValueRemoved.add("updated", updated);

            JsonObject objectContextChanged = new JsonObject();
            objectContextChanged.addProperty("type", CONTEXT_VALUE_REMOVED);
            objectContextChanged.add("updated", updated);

            waitForIt.emit(this, CONTEXT_VALUE_REMOVED, objectValueRemoved);
            waitForIt.emit(this, CONTEXT_CHANGED, objectContextChanged);
        }
        return removed;
    }

    private boolean removeValueForKey(String key, JsonObject map) {
        return recurseRemoveValue(key.split(Pattern.quote(".")), 0, map);
    }

    private boolean recurseRemoveValue(String[] keys, int index, JsonObject map) {

        String key = keys[index];
        if (index == (keys.length - 1)) {
            map.remove(key);
            return true;
        }

        if (!(map.has(key))) {
            return false;
        }

        boolean removed = recurseRemoveValue(keys, index + 1, map.get(key).getAsJsonObject());
        if (removed && map.get(key).getAsJsonObject().keySet().size() == 0) {
            map.remove(key);
        }
        return removed;
    }
}
