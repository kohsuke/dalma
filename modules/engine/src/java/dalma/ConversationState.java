package dalma;

/**
 * State of a {@link Conversation}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ConversationState {
    private final String value;

    public final boolean isRemovable;

    /**
     * {@link Conversation} can be executed, and currently waiting for an available
     * {@link Executor}.
     */
    public static final ConversationState RUNNABLE = new ConversationState("runnable",false);

    /**
     * {@link Conversation} is being executed by an {@link Executor}.
     */
    public static final ConversationState RUNNING = new ConversationState("running",true);

    /**
     * {@link Conversation} is waiting for an event. Its state is written to a disk
     * to minimize the resource consumption.
     */
    public static final ConversationState SUSPENDED = new ConversationState("suspended",true);

    /**
     * {@link Conversation} has finished its execution normally.
     */
    public static final ConversationState ENDED = new ConversationState("ended",false);

    /**
     * {@link Conversation} has been terminated with the {@link Conversation#remove()} method.
     */
    public static final ConversationState ABORTED = new ConversationState("aborted",false);


    private ConversationState(String value,boolean removable) {
        this.value = value;
        this.isRemovable = removable;
    }

    public String toString() {
        return value;
    }
}
