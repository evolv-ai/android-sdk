package ai.evolv.android_sdk.evolvinterface;

public interface EvolvInvocation<T> {

    /**
     * Invoke a given value to a set of instructions.
     * @param value any value that was requested
     */
    void invoke(T value);

}
