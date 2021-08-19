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
    JsonElement get(String key);


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
     * Marks a consumer as unsuccessfully retrieving and / or applying requested values, making them ineligible for inclusion in optimization statistics.
     *
     * @param details   Optional. Information on the reason for contamination. If provided, the object should contain a reason. Optionally, a 'details' value should be included for extra debugging info
     * @param allExperiments If true, the user will be excluded from all optimizations, including optimization not applicable to this page
     */
    void contaminate(JsonObject details, boolean allExperiments);

    /**
     * Check all active keys that start with the specified prefix.
     *
     * @param prefix a unique key identifying a specific value in the participants
     *               allocation
     */
    JsonObject getActiveKeys(String prefix);

    /**
     * Check all active keys that start with the specified prefix.
     * allocation
     */
    JsonObject getActiveKeys();

    JsonElement activeEntryPoints();

    //test subscribeActiveKeys
    void subscribeActiveKeys(String prefix,EvolvAction action);

    //test subscribeGet
    void subscribeGet(String key,String defaultValue, EvolvAction action);

    void subscribeIsActive(String key, EvolvAction action);

    void subscribeActiveEntryPoints(EvolvAction action);

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
     * Add listeners to lifecycle events that take place in to client.
     *
     * Currently supported events:
     *
     *     "initialized" - Called when the client is fully initialized and ready for use with (topic, options)
     *     "context.initialized" - Called when the context is fully initialized and ready for use with (topic, updated_context)
     *     "context.changed" - Called whenever a change is made to the context values with (topic, updated_context)
     *     "context.value.removed" - Called when a value is removed from context with (topic, key, updated_context)
     *     "context.value.added" - Called when a new value is added to the context with (topic, key, value, local, updated_context)
     *     "context.value.changed" - Called when a value is changed in the context (topic, key, value, before, local, updated_context)
     *     "context.destroyed" - Called when the context is destroyed with (topic, context)
     *     "genome.request.sent" - Called when a request for a genome is sent with (topic, requested_keys)
     *     "config.request.sent" - Called when a request for a config is sent with (topic, requested_keys)
     *     "genome.request.received" - Called when the result of a request for a genome is received (topic, requested_keys)
     *     "config.request.received" - Called when the result of a request for a config is received (topic, requested_keys)
     *     "request.failed" - Called when a request fails (topic, source, requested_keys, error)
     *     "genome.updated" - Called when the stored genome is updated (topic, allocation_response)
     *     "config.updated" - Called when the stored config is updated (topic, config_response)
     *     "effective.genome.updated" - Called when the effective genome is updated (topic, effectiveGenome)
     *     "store.destroyed" - Called when the store is destroyed (topic, store)
     *     "confirmed" - Called when the consumer is confirmed (topic)
     *     "contaminated" - Called when the consumer is contaminated (topic)
     *     "event.emitted" - Called when an event is emitted through the beacon (topic, type, score)
     *
     * @param topic     The event topic on which the listener should be invoked.
     * @param listener  The listener to be invoked for the specified topic.
     */
    void on(String topic, EvolvInvocation listener);

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
