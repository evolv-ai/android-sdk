package ai.evolv.android_sdk;

import com.google.gson.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

class ExecutionQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionQueue.class);

    private final ConcurrentLinkedQueue<Execution> queue;

    ExecutionQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    void enqueue(Execution execution) {
        this.queue.add(execution);
    }

    void executeAllWithValuesFromAllocations(JsonArray allocations,
                                             boolean confirmationSandbagged,
                                             boolean contaminationSandbagged) {
        while (!queue.isEmpty()) {
            Execution execution = queue.remove();
            try { } catch (Exception e) {
                LOGGER.error("There was an issue while performing one of" +
                        " the stored actions.", e);
            }
        }
    }

    void executeAllWithValuesFromDefaults() {
        while (!queue.isEmpty()) {
            Execution execution = queue.remove();
            execution.executeWithDefault();
        }
    }
}
