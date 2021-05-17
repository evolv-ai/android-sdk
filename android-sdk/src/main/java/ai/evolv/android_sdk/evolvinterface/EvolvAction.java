package ai.evolv.android_sdk.evolvinterface;

public interface EvolvAction<T> {

    /**
     * Applies a given value to a set of instructions.
     * @param value any value that was requested
     */
    void apply(T value);

}
