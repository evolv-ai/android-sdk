package ai.evolv.android_sdk;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvAllocationStore;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.exceptions.EvolvKeyError;
import ai.evolv.android_sdk.generics.GenericClass;

class EvolvClientImpl implements EvolvClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvolvClientImpl.class);

    private final EventEmitter eventEmitter;
    private final ListenableFuture<JsonArray> futureAllocations;
    private final ListenableFuture<JsonObject> futureConfiguration;
    private final ExecutionQueue executionQueue;
    private final Allocator allocator;
    private final EvolvAllocationStore store;
    private final boolean previousAllocations;
    private final EvolvParticipant participant;

    EvolvClientImpl(EvolvConfig config,
                    EventEmitter emitter,
                    ListenableFuture<JsonArray> futureAllocations,
                    ListenableFuture<JsonObject> futureConfiguration,
                    Allocator allocator,
                    boolean previousAllocations,
                    EvolvParticipant participant) {
        this.store = config.getEvolvAllocationStore();
        this.executionQueue = config.getExecutionQueue();
        this.eventEmitter = emitter;
        this.futureAllocations = futureAllocations;
        this.futureConfiguration = futureConfiguration;
        this.allocator = allocator;
        this.previousAllocations = previousAllocations;
        this.participant = participant;
        init();
    }

    private void init() {

    }

    @Override
    public <T> T get(String key, T defaultValue) {
        try {
            if (futureAllocations == null) {
                return defaultValue;
            }

            // this is blocking
            JsonArray allocations = futureAllocations.get();
            if (!Allocator.allocationsNotEmpty(allocations)) {
                return defaultValue;
            }

            GenericClass<T> cls = new GenericClass(defaultValue.getClass());
            T value = new Allocations(allocations, store).getValueFromAllocations(key, cls.getMyType(),
                    participant);

            if (value == null) {
                throw new EvolvKeyError("Got null when retrieving key from allocations.");
            }

            return value;
        } catch (EvolvKeyError e) {
            LOGGER.debug("Unable to retrieve the treatment. Returning " +
                    "the default.", e);
            return defaultValue;
        } catch (Exception e) {
            LOGGER.error("An error occurred while retrieving the treatment. Returning " +
                    "the default.", e);
            return defaultValue;
        }
    }

    @Override
    public <T> void subscribe(String key, T defaultValue, EvolvAction<T> function) {
        Execution execution = new Execution<>(key, defaultValue, function, participant, store);
        if (previousAllocations) {
            try {
                JsonArray allocations = store.get(participant.getUserId());
                execution.executeWithAllocation(allocations);
            } catch (EvolvKeyError e) {
                LOGGER.debug("Unable to retrieve the value of %s from the allocation.",
                        execution.getKey());
                execution.executeWithDefault();
            } catch (Exception e) {
                LOGGER.error("There was an error when applying the stored treatment.", e);
            }
        }

        Allocator.AllocationStatus allocationStatus = allocator.getAllocationStatus();
        if (allocationStatus == Allocator.AllocationStatus.FETCHING) {
            executionQueue.enqueue(execution);
            return;
        } else if (allocationStatus == Allocator.AllocationStatus.RETRIEVED) {
            try {
                JsonArray allocations = store.get(participant.getUserId());
                execution.executeWithAllocation(allocations);
                return;
            } catch (EvolvKeyError e) {
                LOGGER.debug(String.format("Unable to retrieve" +
                        " the value of %s from the allocation.",  execution.getKey()), e);
            } catch (Exception e) {
                LOGGER.error("There was an error applying the subscribed method.", e);
            }
        }

        execution.executeWithDefault();
    }

    @Override
    public void emitEvent(String key, Double score) {
        this.eventEmitter.emit(key, score);
    }

    @Override
    public void emitEvent(String key) {
        this.eventEmitter.emit(key);
    }

    @Override
    public void confirm() {
        Allocator.AllocationStatus allocationStatus = allocator.getAllocationStatus();
        if (allocationStatus == Allocator.AllocationStatus.FETCHING) {
            allocator.sandBagConfirmation();
        } else if (allocationStatus == Allocator.AllocationStatus.RETRIEVED) {
            eventEmitter.confirm(store.get(participant.getUserId()));
        }
    }

    @Override
    public void contaminate() {
        Allocator.AllocationStatus allocationStatus = allocator.getAllocationStatus();
        if (allocationStatus == Allocator.AllocationStatus.FETCHING) {
            allocator.sandBagContamination();
        } else if (allocationStatus == Allocator.AllocationStatus.RETRIEVED) {
            eventEmitter.contaminate(store.get(participant.getUserId()));
        }
    }

    @Override
    public <T> T getActiveKeys(String prefix, T defaultValue) {
        try {
            if (futureAllocations == null) {
                return defaultValue;
            }

            // this is blocking
            JsonArray allocations = futureAllocations.get();
            if (!Allocator.allocationsNotEmpty(allocations)) {
                return defaultValue;
            }

            GenericClass<T> cls = new GenericClass(defaultValue.getClass());
            T value = new Allocations(allocations, store)
                    .getActiveKeysFromAllocations(prefix, cls.getMyType(),
                    participant);

            if (value == null) {
                throw new EvolvKeyError("Got null when retrieving key from allocations.");
            }

            return value;
        } catch (EvolvKeyError e) {
            LOGGER.debug("Unable to retrieve the treatment. Returning " +
                    "the default.", e);
            return defaultValue;
        } catch (Exception e) {
            LOGGER.error("An error occurred while retrieving the treatment. Returning " +
                    "the default.", e);
            return defaultValue;
        }
    }
}
