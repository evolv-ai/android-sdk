package ai.evolv.android_sdk.evolvinterface;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public interface EvolvContext {

    void initialize(String uid,
                    JsonObject remoteContext,
                    JsonObject localContext);


    /**
     * Sets a value in the current context.
     * <p>
     *      Note: This will cause the effective genome to be recomputed.
     * </p>
     * @param key The key to associate the value to.
     * @param value The value to associate with the key.
     * @param local If true, the value will only be added to the localContext.
     * @return the changes were made correctly
     */
    boolean set(String key, Object value, boolean local);

    /**
     * Computes the effective context from the local and remote contexts.
     * @return The effective context from the local and remote contexts.
     */
    JsonObject resolve();

    /**
     * Merge the specified object into the current context.
     * <p>
     *      Note: This will cause the effective genome to be recomputed.
     * </p>
     * @param update The values to update the context with.
     * @param local If true, the values will only be added to the localContext.
     */
    void update(JsonObject update, boolean local);

    /**
     * Remove a specified key from the context.
     * <p>
     *      Note: This will cause the effective genome to be recomputed.
     * </p>
     * @param key The key to remove from the context.
     */
    boolean remove(String key);

    /**
     * Retrieve a value from the context.
     * @param key The kay associated with the value to retrieve.
     */
    JsonElement get(String key);

    /**
     * Checks if the specified key is currently defined in the context.
     * @param key The key to check.
     */
    boolean contains(String key);

    /**
     * Adds value to specified array in context. If array doesnt exist its created and added to.
     * @param key The array to add to.
     * @param value Value to add to the array.
     * @param local If true, the value will only be added to the localContext.
     */
    boolean pushToArray(String key, String value, boolean local);

    /**
     * Adds value to specified array in context. If array doesnt exist its created and added to.
     * @param key The array to add to.
     * @param value Value to add to the array.
     * @param local If true, the value will only be added to the localContext.
     * @param limit Max length of array to maintain.
     */
    boolean pushToArray(String key, String value, boolean local, int limit);

}
