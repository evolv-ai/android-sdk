package ai.evolv.android_sdk;

import com.google.gson.JsonArray;

import java.util.HashSet;
import java.util.Set;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.generics.GenericClass;

class Execution<T> {

    private final String key;
    private final T defaultValue;
    private final EvolvAction function;
    private final EvolvParticipant participant;
    private final EvolvAllocationStore store;

    private Set<String> alreadyExecuted = new HashSet<>();

    Execution(String key, T defaultValue, EvolvAction<T> function, EvolvParticipant participant,
              EvolvAllocationStore store) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.function = function;
        this.participant = participant;
        this.store = store;
    }

    String getKey() {
        return key;
    }

    void executeWithAllocation(JsonArray rawAllocations) throws EvolvKeyError {
        GenericClass<T> cls = new GenericClass(defaultValue.getClass());
        Allocations allocations = new Allocations(rawAllocations, store);
        T value = allocations.getValueFromAllocations(key, cls.getMyType(), participant);

        if (value == null) {
            throw new EvolvKeyError("Got null when retrieving key from allocations.");
        }

        Set<String> activeExperiments = allocations.getActiveExperiments();
        if (alreadyExecuted.isEmpty() || !alreadyExecuted.equals(activeExperiments)) {
            // there was a change to the allocations after reconciliation, apply changes
            function.apply(value);
        }

        alreadyExecuted = activeExperiments;
    }

    void executeWithDefault() {
        function.apply(defaultValue);
    }

}
