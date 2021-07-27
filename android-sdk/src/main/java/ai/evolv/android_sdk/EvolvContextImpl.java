package ai.evolv.android_sdk;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.helper.UtilityHelper;

import static ai.evolv.android_sdk.EvolvStoreImpl.EMPTY_STRING;

class EvolvContextImpl implements EvolvContext {

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
    private UtilityHelper helper = new UtilityHelper();
    private boolean initialized = false;
    private EvolvConfig evolvConfig;
    private EvolvStoreImpl evolvStore;
    private WaitForIt waitForIt;
    //private CopyOnWriteArrayList<String> flattened = new CopyOnWriteArrayList<>();
    private JsonObject flattened = new JsonObject();

    public EvolvContextImpl(EvolvStoreImpl evolvStore, WaitForIt waitForIt) {
        this.evolvStore = evolvStore;
        this.waitForIt = waitForIt;
    }

    public JsonObject getRemoteContext() {
        return remoteContext;
    }

    public void setRemoteContext(JsonObject remoteContext) {
        this.remoteContext = remoteContext ;
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
        // TODO: 13.07.2021 remove
//        List<Object> objects = new ArrayList<>();
//        objects.add(CONTEXT_INITIALIZED);
//        objects.add(resolve);

        JsonObject object = new JsonObject();
        object.addProperty("type",CONTEXT_INITIALIZED);
        object.add("value",resolve);


        waitForIt.emit(this, CONTEXT_INITIALIZED, object);
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

        JsonObject updated = this.resolve();

        if (before == null ) {

            JsonObject objects = new JsonObject();
            objects.addProperty("type",CONTEXT_VALUE_ADDED);
            objects.addProperty("key",key);
            objects.add("value",jsonValue);
            objects.addProperty("local",false);
            objects.add("updated",updated);


            waitForIt.emit(this, CONTEXT_VALUE_ADDED, objects);
        } else {

            JsonObject objects = new JsonObject();
            objects.addProperty("type",CONTEXT_VALUE_CHANGED);
            objects.addProperty("key",key);
            objects.add("value",jsonValue);
            objects.add("before",before);
            objects.addProperty("local",false);
            objects.add("updated",updated);


            waitForIt.emit(this, CONTEXT_VALUE_CHANGED, objects);
        }

        waitForIt.emit(this, CONTEXT_CHANGED, updated);
        return true;
    }

    // TODO: 27.07.2021 testing! (used in the store)
    @Override
    public JsonObject resolve() {
        ensureInitialized();
// TODO: 26.07.2021 uncomment and need to test!
//        for (Map.Entry<String, JsonElement> entry : localContext.entrySet()) {
//            remoteContext.add(entry.getKey(),entry.getValue());
//        }
//
//        return remoteContext;

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

    @Override
    public JsonElement get(String key) {
        ensureInitialized();

        if (key.equals("confirmations") || key.equals("contaminations")) {
            LOGGER.error("[Deprecation] Retrieving confirmations and contaminations from the Evolv context with keys \"confirmations\"," +
                    "and \"contaminations\" is deprecated. Please use \"experiments.confirmations\" and \"experiments.contaminations\" instead.");
        }
// TODO: 27.07.2021 question: JS SDK - "return objects.getValueForKey(key, remoteContext) || objects.getValueForKey(key, localContext);"
//  need to understand which cases should return value local or remote (if local and remote contexts aren't emprty)

        if(localContext.size() != 0){
            return helper.getValueForKey(key, localContext).getAsJsonObject();
        }


        JsonElement element = helper.getValueForKey(key, remoteContext);
        return element;

    }

    // TODO: 27.07.2021 need to test!
    @Override
    public boolean pushToArray(String key, String value, boolean local) {
        int limit = DEFAULT_QUEUE_LIMIT;

        ensureInitialized();
        JsonObject context = local ? localContext : remoteContext;

        JsonArray newArray = new JsonArray();
        JsonObject element = new JsonObject();

        JsonElement originalArray = helper.getValueForKey(key, context);

        if(originalArray == null){
            LOGGER.error("The " + "\"" + key + "\"" +  " does not have a suitable value ");
            return false;
        }else if(originalArray.isJsonPrimitive()){
            element.addProperty(key,value);
        }else if(originalArray.isJsonObject()){
            element.add(key,originalArray);
        }

        newArray.add(element);

        for (int i = 0; i < newArray.size() ; i++) {
            if(i > limit){
                newArray.remove(i);
            }
        }
        return set(key,newArray,local);
    }

    @Override
    public boolean pushToArray(String key, String value, boolean local, int limit) {
        ensureInitialized();
        JsonObject context = local ? localContext : remoteContext;

        JsonArray newArray = new JsonArray();
        JsonObject element = new JsonObject();

        JsonElement originalArray = helper.getValueForKey(key, context);

        if(originalArray == null){
            LOGGER.error("The " + "\"" + key + "\"" +  " does not have a suitable value ");
            return false;
        }else if(originalArray.isJsonPrimitive()){
            element.addProperty(key,value);
        }else if(originalArray.isJsonObject()){
            element.add(key,originalArray);
        }

        newArray.add(element);

        for (int i = 0; i < newArray.size() ; i++) {
            if(i > limit){
               newArray.remove(i);
            }
        }
        return set(key,newArray,local);
    }

    @Override
    public boolean contains(String key) {
        ensureInitialized();
        return remoteContext.has(key) || localContext.has(key);
    }

    // TODO: 26.07.2021 need to test!
    @Override
    public void update(JsonObject update, boolean local) {
        ensureInitialized();
        JsonObject context = local ? localContext : remoteContext;
        flatten(update);

        JsonObject flattenedBefore = new JsonObject();

        for (String key : flattened.keySet()) {
            flattenedBefore.add(key,context.get(key));
        }

        if (local) {

            for (Map.Entry<String, JsonElement> entry : update.entrySet()) {
                localContext.add(entry.getKey(),entry.getValue());
            }
            context = localContext;
        } else {
            for (Map.Entry<String, JsonElement> entry : update.entrySet()) {
                remoteContext.add(entry.getKey(),entry.getValue());
            }
            context = remoteContext;
        }

        JsonObject updated = this.resolve();

        for (String key : flattened.keySet()) {

            if (flattenedBefore.get(key) == null) {

                JsonObject object = new JsonObject();
                object.addProperty("type",CONTEXT_INITIALIZED);
                object.addProperty("key",key);
                object.add("flattened_key",flattened.get(key));
                object.addProperty("local",local);
                object.add("updated",updated);

                waitForIt.emit(this, CONTEXT_VALUE_ADDED, object);
            }else if (flattenedBefore.get(key) != context.get(key)) {

                JsonObject object = new JsonObject();
                object.addProperty("type",CONTEXT_INITIALIZED);
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
        recurseflatten(map, "");
    }

    private String recurseflatten(JsonElement current, String parentKey) {
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
                        items.add(key.concat(recurseflatten(element, newKey)));
                    }
                }
            }
        }

        return EMPTY_STRING;
    }

    EvolvStoreImpl.Filter<String> startsWithFilter = key -> !key.startsWith("_")
            || key.equals("_values")
            || key.equals("_initializers");

    // TODO: 27.07.2021 need to test!
    @Override
    public boolean remove(String key) {
        ensureInitialized();

        boolean local = removeValueForKey(key, localContext);
        boolean remote = removeValueForKey(key, remoteContext);

        boolean removed = local || remote;

        if(removed){
           JsonObject updated = this.resolve();

            JsonObject objectValueRemoved = new JsonObject();
            objectValueRemoved.addProperty("type",CONTEXT_VALUE_REMOVED);
            objectValueRemoved.addProperty("key",key);
            objectValueRemoved.addProperty("remote",!remote);
            objectValueRemoved.add("updated",updated);

            JsonObject objectContextChanged = new JsonObject();
            objectContextChanged.addProperty("type",CONTEXT_VALUE_REMOVED);
            objectContextChanged.add("updated",updated);

            waitForIt.emit(this, CONTEXT_VALUE_REMOVED,objectValueRemoved);
            waitForIt.emit(this, CONTEXT_CHANGED, objectContextChanged);
        }

        return removed;

    }

    private boolean removeValueForKey(String key, JsonObject map) {
        recurseRemoveValue(key.split(Pattern.quote(".")),0,map);

        return false;
    }

    private boolean recurseRemoveValue(String[] keys, int index, JsonObject map) {

        String key = keys[index];
        if (index == (keys.length - 1)) {
            map.remove(key);
            return true;
        }

        if (!(map.has(key))){
            return false;
        }

    boolean removed = recurseRemoveValue(keys, index + 1, map.get(key).getAsJsonObject());
        if (removed &&  map.get(key).getAsJsonObject().keySet().size() == 0) {
            map.remove(key);
        }
        return removed;
    }

}
