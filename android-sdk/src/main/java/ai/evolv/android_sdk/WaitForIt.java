package ai.evolv.android_sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.evolvinterface.EvolvInvocation;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_ADDED;
import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_VALUE_REMOVED;

class WaitForIt {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForIt.class);

    Map<Object, Map<String, List<Object>>> scopedHandlers = new LinkedHashMap<>();
    Map<Object, Map<String, List<Object>>> scopedOnceHandlers = new LinkedHashMap<>();
    Map<Object, Map<String, List<Object>>> scopedPayloads = new LinkedHashMap<>();

    void ensureScope(Object scope) {
        if (scopedHandlers.containsKey(scope)) {
            return;
        }

        Map<String, List<Object>> scopedHandlersValueMap = new LinkedHashMap<>();
        Map<String, List<Object>> scopedOnceHandlerValueMap = new LinkedHashMap<>();
        Map<String, List<Object>> scopedPayloadsValueMap = new LinkedHashMap<>();

        scopedHandlers.put(scope, scopedHandlersValueMap);
        scopedOnceHandlers.put(scope, scopedOnceHandlerValueMap);
        scopedPayloads.put(scope, scopedPayloadsValueMap);
    }

    void waitFor(Object scope, String it, Object handler) {
        ensureScope(scope);

        Map<String, List<Object>> handlers = scopedHandlers.get(scope);
        Map<String, List<Object>> payloads = scopedPayloads.get(scope);

        List<Object> objects = new ArrayList<>();

        objects.add(handler);
        handlers.put(it, objects);

        if (payloads.containsKey(it)) {
            if (handler instanceof EvolvInvocation) {
                ((EvolvInvocation) handler).invoke(payloads.get(it));
            }
        }
    }

//    void waitFor(Object scope, String it, Object handler) {
//        ensureScope(scope);
//
//        Map<String, List<Object>> handlers = scopedHandlers.get(scope);
//        Map<String, List<Object>> payloads = scopedPayloads.get(scope);
//
//        List<Object> objects = new ArrayList<>();
//
//        objects.add(handler);
//        handlers.put(it, objects);
//
//        if (payloads.containsKey(it)) {
//            if (handler instanceof EvolvInvocation) {
//                ((EvolvInvocation) handler).invoke(payloads.get(it));
//            }
//        }
//    }

    void emit(Object scope, String it, List<Object> list) {
        ensureScope(scope);

        Map<String, List<Object>> handlers = scopedHandlers.get(scope);
        Map<String, List<Object>> onceHandlers = scopedOnceHandlers.get(scope);
        Map<String, List<Object>> payloads = scopedPayloads.get(scope);

        payloads.put(it, list);

        List<Object> handlersForIt = handlers.get(it);
        if (handlersForIt == null) {
            return;
        }

        for (Object o : handlersForIt) {
            try {
                //call function
                //h.apply(undefined, payload);
                //((EvolvAction)evolvAction).apply();

            } catch (Exception e) {
                LOGGER.error("Failed to invoke handler of " + it, e);
            }
        }
    }
}
