package ai.evolv.android_sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;

class Allocations {

    private static final String TOUCHED = "touched";
    private static final String CONFIRMED = "confirmed";
    private static final String CONTAMINATED = "contaminated";

    private static final Logger LOGGER = LoggerFactory.getLogger(Allocations.class);

    private final JsonArray allocations;
    private final EvolvAllocationStore store;

    private final Audience audience = new Audience();

    Allocations(JsonArray allocations, EvolvAllocationStore store) {
        this.allocations = allocations;
        this.store = store;
    }

    <T> T getValueFromAllocations(String key, Class<T> cls, EvolvParticipant participant)
            throws EvolvKeyError {
        ArrayList<String> keyParts = new ArrayList<>(Arrays.asList(key.split("\\.")));
        if (keyParts.isEmpty()) {
            throw new EvolvKeyError("Key provided was empty.");
        }

        for (JsonElement a : allocations) {
            JsonObject allocation = a.getAsJsonObject();
            if (!audience.filter(participant.getUserAttributes(), allocation)) {
                try {
                    JsonElement element = getElementFromGenome(allocation.get("genome"), keyParts);
                    T value = new Gson().fromJson(element, cls);
                    if (value != null) {
                        LOGGER.debug(String.format("Found value for key '%s' in experiment %s",
                                key, allocation.get("eid").getAsString()));
                        markTouched(allocation);
                        store.put(participant.getUserId(), allocations);
                    }
                    return value;
                } catch (EvolvKeyError e) {
                    LOGGER.debug(String.format("Unable to find key '%s' in experiment %s.",
                            key, allocation.get("eid").getAsString()));
                    continue;
                }
            }

            LOGGER.debug(String.format("Participant was filtered from experiment %s",
                    allocation.get("eid").getAsString()));
        }

        throw new EvolvKeyError(String.format("No value was found in any allocations for key: %s",
                keyParts.toString()));
    }

    private JsonElement getElementFromGenome(JsonElement genome, List<String> keyParts)
            throws EvolvKeyError {
        JsonElement element = genome;
        if (element == null) {
            throw new EvolvKeyError("Allocation genome was empty.");
        }

        for (String part : keyParts) {
            JsonObject object = element.getAsJsonObject();
            element = object.get(part);
            if (element == null) {
                throw new EvolvKeyError("Could not find value for key: " + keyParts.toString());
            }
        }

        return element;
    }

    <T> T getActiveKeysFromAllocations(String prefix, Class<T> cls, EvolvParticipant participant)
            throws EvolvKeyError {

        ArrayList<String> keyParts = new ArrayList<>(Arrays.asList(prefix.split("\\.")));

        for (JsonElement a : allocations) {
            JsonObject allocation = a.getAsJsonObject();
            if (!audience.filter(participant.getUserAttributes(), allocation)) {
                try {
                    JsonArray elements = getActiveKeysFromGenome(allocation.get("genome"), keyParts);
                    T value = new Gson().fromJson(elements.toString(),cls);
                    if (value != null) {
                        LOGGER.debug(String.format("Found value for active keys '%s' in experiment %s",
                                prefix, allocation.get("eid").getAsString()));
                        markTouched(allocation);
                        store.put(participant.getUserId(), allocations);
                    }
                    return value;
                } catch (EvolvKeyError e) {
                    LOGGER.debug(String.format("Unable to find active keys '%s' in experiment %s.",
                            prefix, allocation.get("eid").getAsString()));
                    continue;
                }
            }

            LOGGER.debug(String.format("Participant was filtered from experiment %s",
                    allocation.get("eid").getAsString()));
        }

        throw new EvolvKeyError(String.format("No value was found in any allocations for key: %s",
                keyParts.toString()));

    }

    private JsonArray getActiveKeysFromGenome(JsonElement genome, List<String> keyParts)
            throws EvolvKeyError {
        JsonElement element = genome;
        JsonArray jsonElements = new JsonArray();
        if (element == null) {
            throw new EvolvKeyError("Allocation genome was empty.");
        }

        if (keyParts.size() == 1 && keyParts.get(0).equals("")) {

            JsonObject object = element.getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : object.entrySet()){

                String key = entry.getKey();
                JsonObject entryValue = entry.getValue().getAsJsonObject();
                jsonElements.add(key);

                    for(Map.Entry<String, JsonElement> entry2 : entryValue.entrySet()){
                        jsonElements.add(key + "." + entry2.getKey());
                    }

            }

        } else {
            for (String part : keyParts) {
                JsonObject object = element.getAsJsonObject();
                element = object.get(part);
                jsonElements.add(element);
                if (element == null) {
                    throw new EvolvKeyError("Could not find value for key: " + keyParts.toString());
                }
            }
        }

        return jsonElements;
    }

    /**
     * Reconciles the previous allocations with any new allocations.
     *
     * <p>
     * Check the current allocations for any allocations that belong to experiments
     * in the previous allocations. If there are, keep the previous allocations.
     * If there are any live experiments that are not in the previous allocations
     * add the new allocation to the allocations list.
     * </p>
     *
     * @param previousAllocations the stored allocations
     * @param currentAllocations  the allocations recently fetched
     * @return the reconcile allocations
     */
    static JsonArray reconcileAllocations(JsonArray previousAllocations,
                                          JsonArray currentAllocations) {
        JsonArray allocations = new JsonArray();

        for (JsonElement ca : currentAllocations) {
            JsonObject currentAllocation = ca.getAsJsonObject();
            String currentEid = currentAllocation.get("eid").toString();
            boolean previousFound = false;

            for (JsonElement pa : previousAllocations) {
                JsonObject previousAllocation = pa.getAsJsonObject();
                String previousEid = previousAllocation.get("eid").toString();

                if (previousEid.equals(currentEid)) {
                    allocations.add(pa.getAsJsonObject());
                    previousFound = true;
                }
            }

            if (!previousFound) {
                allocations.add(ca.getAsJsonObject());
            }
        }

        return allocations;
    }

    Set<String> getActiveExperiments() {
        Set<String> activeExperiments = new HashSet<>();
        for (JsonElement a : allocations) {
            JsonObject allocation = a.getAsJsonObject();
            activeExperiments.add(allocation.get("eid").getAsString());
        }
        return activeExperiments;
    }

    static JsonObject markTouched(JsonObject allocation) {
        allocation.addProperty(TOUCHED, true);
        return allocation;
    }

    static boolean isTouched(JsonObject allocation) {
        return allocation.has(TOUCHED) &&
                allocation.get(TOUCHED).getAsBoolean();
    }

    static JsonObject markConfirmed(JsonObject allocation) {
        allocation.addProperty(CONFIRMED, true);
        return allocation;
    }

    static boolean isConfirmed(JsonObject allocation) {
        return allocation.has(CONFIRMED) &&
                allocation.get(CONFIRMED).getAsBoolean();
    }

    static JsonObject markContaminated(JsonObject allocation) {
        allocation.addProperty(CONTAMINATED, true);
        return allocation;
    }

    static boolean isContaminated(JsonObject allocation) {
        return allocation.has(CONTAMINATED) &&
                allocation.get(CONTAMINATED).getAsBoolean();
    }

}
