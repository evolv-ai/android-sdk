package ai.evolv.android_sdk.evolvinterface;

import com.google.gson.JsonElement;

import java.util.Map;

public interface EvolvContext {

    void initialize(String uid,
                    Map<String, Object> remoteContext,
                    Map<String, Object> localContext);


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
    JsonElement resolve();


}
