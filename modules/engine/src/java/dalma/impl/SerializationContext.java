package dalma.impl;

/**
 * Designates the current mode of serialization.
 *
 * <p>
 * There are two different serializations.
 *
 * 1. serialization of the continuation
 * 2. serialization of the conversation state
 *
 * @author Kohsuke Kawaguchi
 */
final class SerializationContext {
    final EngineImpl engine;
    final Mode mode;

    private SerializationContext(EngineImpl engine,Mode mode) {
        this.mode = mode;
        this.engine = engine;
    }

    public static enum Mode {
        /**
         * De-hydration of the continuation
         */
        CONTINUATION,
        /**
         * Saving the state of the conversation
         */
        CONVERSATION,
        /**
         * Saving the state of the engine
         */
        ENGINE;
    }

    public static SerializationContext get() {
        return SERIALIZATION_CONTEXT.get();
    }

    /*package*/ static SerializationContext set(EngineImpl engine, Mode mode) {
        SerializationContext old = SERIALIZATION_CONTEXT.get();
        SERIALIZATION_CONTEXT.set(new SerializationContext(engine,mode));
        return old;
    }

    /*package*/ static void remove() {
        SERIALIZATION_CONTEXT.set(null);
    }

    /**
     * While the hydration of the conversation is in progress,
     * this variable stores the {@link EngineImpl} that owns the conversation.
     *
     * <p>
     * This is used to resolve serialized instances to running instances.
     */
    /*package*/ static final ThreadLocal<SerializationContext> SERIALIZATION_CONTEXT
        = new ThreadLocal<SerializationContext>();
}
