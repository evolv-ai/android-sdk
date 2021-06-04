package ai.evolv.android_sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;

class WaitForIt<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForIt.class);

    Map<Object, Map<String, List<EvolvInvocation<T>>>> scopedHandlers = new LinkedHashMap<>();
    Map<Object, Map<String, List<EvolvInvocation<T>>>> scopedOnceHandlers = new LinkedHashMap<>();
    Map<Object, Map<String, T>> scopedPayloads = new LinkedHashMap<>();

    void ensureScope(Object scope) {
        if (scopedHandlers.containsKey(scope)) {
            return;
        }

        Map<String, List<EvolvInvocation<T>>> scopedHandlersValueMap = new LinkedHashMap<>();
        Map<String, List<EvolvInvocation<T>>> scopedOnceHandlerValueMap = new LinkedHashMap<>();
        Map<String, T> scopedPayloadsValueMap = new LinkedHashMap<>();

        scopedHandlers.put(scope, scopedHandlersValueMap);
        scopedOnceHandlers.put(scope, scopedOnceHandlerValueMap);
        scopedPayloads.put(scope, scopedPayloadsValueMap);

    }

    void destroyScope(Object scope) {
        scopedHandlers.clear();
        scopedOnceHandlers.clear();
        scopedPayloads.clear();
    }

    // TODO: 04.06.2021 need to create test
    void waitFor(Object scope, String it, EvolvInvocation<T> handler) {
        ensureScope(scope);

        Map<String, List<EvolvInvocation<T>>> handlers = scopedHandlers.get(scope);
        Map<String, T> payloads = scopedPayloads.get(scope);

        List<EvolvInvocation<T>> list = new ArrayList<>();
        list.add(handler);

        handlers.put(it, list);

        if (payloads.containsKey(it)) {
               handler.invoke(payloads.get(it));
        }
    }

    // TODO: 04.06.2021 need to create test and debug it
    void waitOnceFor(Object scope, String it, EvolvInvocation<T> handler) {
        ensureScope(scope);

        Map<String, List<EvolvInvocation<T>>> handlers = scopedOnceHandlers.get(scope);
        Map<String, T> payloads = scopedPayloads.get(scope);

        if (payloads.containsKey(it)) {
            handler.invoke(payloads.get(it));
            return;
        }

        List<EvolvInvocation<T>> list = new ArrayList<>();
        list.add(handler);

        handlers.put(it, list);
    }

    // TODO: 04.06.2021 need to create test
    void emit(Object scope, String it, T payloadList) {
        ensureScope(scope);

        Map<String, List<EvolvInvocation<T>>> handlers = scopedHandlers.get(scope);
        Map<String, List<EvolvInvocation<T>>> onceHandlers = scopedOnceHandlers.get(scope);
        Map<String, T> payloads = scopedPayloads.get(scope);

        T payload = payloadList;
        payloads.put(it, payload);

        // TODO: 03.06.2021 add "onceHandlers" logic
        
        List<EvolvInvocation<T>> handlersForIt = handlers.get(it);
        if (handlersForIt == null) {
            return;
        }

        for (EvolvInvocation<T> handler : handlersForIt) {
            try {
                handler.invoke( payload);
            } catch (Exception e) {
                LOGGER.error("Failed to invoke handler of " + it, e);
            }
        }
    }
}
