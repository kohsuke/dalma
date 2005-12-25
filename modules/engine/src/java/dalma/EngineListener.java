package dalma;

/**
 * Receives various event notifications that happen in {@link Engine}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class EngineListener {
    /**
     * Invoked when a new {@link Conversation} is created.
     */
    public void onConversationStarted( Conversation conv ) {}

    /**
     * Invoked when a {@link Conversation} is just completed.
     */
    public void onConversationCompleted( Conversation conv ) {}
}
