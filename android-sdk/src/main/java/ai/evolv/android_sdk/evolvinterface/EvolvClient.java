package ai.evolv.android_sdk.evolvinterface;

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Map;

public interface EvolvClient {

    /**
     * Get the value of a specified key.
     *
     * @param key          The key of the value to retrieve.
     * @return a value associated with the given key
     */
    // TODO: 19.07.2021 uncomment (callBack testing)
    //JsonElement get(String key);
    void get(String key,EvolvAction action);

    /**
     * Retrieves a value from Evolv asynchronously and applies some custom action.
     * <p>
     * This method is non blocking. It will preform the programmed action once
     * the allocation is available. If there is already of stored allocation
     * it will immediately apply the value retrieved and then when the new
     * allocation returns it will reapply the new changes if the experiment
     * has changed.
     * </p>
     *
     * @param key          a unique key identifying a specific value in the participants
     *                     allocation
     * @param defaultValue a default value to return upon error
     * @param function     a handler that is invoked when the allocation is updated
     * @param <T>          type of value to be returned
     */
    <T> void subscribe(String key, T defaultValue, EvolvAction<T> function);

    /**
     * Sends a confirmed event to Evolv.
     * <p>
     * Method produces a confirmed event which confirms the participant's
     * allocation. Method will not do anything in the event that the allocation
     * timed out or failed.
     * </p>
     */
    void confirm();

    /**
     * Sends a contamination event to Evolv.
     * <p>
     * Method produces a contamination event which will contaminate the
     * participant's allocation. Method will not do anything in the event
     * that the allocation timed out or failed.
     * </p>
     */
    void contaminate();

    /**
     * Check all active keys that start with the specified prefix.
     *
     * @param prefix a unique key identifying a specific value in the participants
     *               allocation
     */
    // TODO: 19.07.2021 uncomment (callBack testing)
    //JsonObject getActiveKeys(String prefix);
    void getActiveKeys(String prefix,EvolvAction action);

    /**
     * Check all active keys that start with the specified prefix.
     * allocation
     */
    // TODO: 19.07.2021 uncomment (callBack testing)
    //JsonObject getActiveKeys();
    void getActiveKeys(EvolvAction action);



    /**
     * Initializes the client with required context information.
     *
     * @param uid           A globally unique identifier for the current participant.
     * @param remoteContext A map of data used for evaluating context predicates and analytics.
     * @param localContext  A map of data used only for evaluating context predicates.
     */
    void initialize(String uid, JsonObject remoteContext, JsonObject localContext);

    /**
     * Reevaluates the current context..
     */
    void reevaluateContext();

    /**
     * Preload all keys under under the specified prefixes.
     *
     * @param prefixes   A list of prefixes to keys to load.
     * @param configOnly If true, only the config would be loaded.
     * @param immediate  Forces the requests to the server.
     */
    void preload(ArrayList<String> prefixes, boolean configOnly, boolean immediate);

    /**
     * Preload all keys under under the specified prefixes.
     *
     * @param prefixes   A list of prefixes to keys to load.
     * @param configOnly If true, only the config would be loaded.
     *                   immediate - Forces the requests to the server. (default: false)
     */
    void preload(ArrayList<String> prefixes, boolean configOnly);

    /**
     * Preload all keys under under the specified prefixes.
     *
     * @param prefixes A list of prefixes to keys to load.
     *                 immediate - Forces the requests to the server. (default: false)
     *                 configOnly - If true, only the config would be loaded. (default: false)
     */
    void preload(ArrayList<String> prefixes);

    /**
     * Check if a specified key is currently active.
     *
     * @param key The key to check.
     */
    boolean isActive(String key);

    /**
     * Get the configuration for a specified key.
     *
     * @param key The key to retrieve the configuration for.
     */
    JsonElement getConfig(String key);

    /**
     * Clears the active keys to reset the key states.
     *
     * @param prefix The prefix of the keys clear.
     */
    void clearActiveKeys(String prefix);

    /**
     * Clears all active keys to reset the key states.
     *
     */
    void clearActiveKeys();

}
