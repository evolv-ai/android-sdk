package ai.evolv.android_sdk;

import com.google.gson.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

import ai.evolv.android_sdk.exceptions.EvolvKeyError;

class ExecutionQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionQueue.class);

    private final ConcurrentLinkedQueue<Execution> queue;

    ExecutionQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    void enqueue(Execution execution) {
        this.queue.add(execution);
    }

    void executeAllWithValuesFromAllocations(JsonArray allocations, EventEmitter eventEmitter,
                                             boolean confirmationSandbagged,
                                             boolean contaminationSandbagged) {
        while (!queue.isEmpty()) {
            Execution execution = queue.remove();
            try {
                execution.executeWithAllocation(allocations);
            } catch (EvolvKeyError e) {
                LOGGER.debug(String.format("There was an error retrieving" +
                        " the value of %s from the allocation.",  execution.getKey()), e);
                execution.executeWithDefault();
            } catch (Exception e) {
                LOGGER.error("There was an issue while performing one of" +
                        " the stored actions.", e);
            }
        }

        if (confirmationSandbagged) {
            eventEmitter.confirm(allocations);
        }

        if (contaminationSandbagged) {
            eventEmitter.contaminate(allocations);
        }
    }

    void executeAllWithValuesFromDefaults() {
        while (!queue.isEmpty()) {
            Execution execution = queue.remove();
            execution.executeWithDefault();
        }
    }

}
