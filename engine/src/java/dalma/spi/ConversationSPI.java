package dalma.spi;

import dalma.Conversation;
import dalma.impl.EngineImpl;
import dalma.spi.port.EndPoint;
import dalma.spi.port.Dock;

import java.util.List;

/**
 * Additional methods of {@link Conversation} available for {@link EndPoint}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ConversationSPI implements Conversation {
    /**
     * Suspends the conversation.
     *
     * @param dock
     *      The dock to which this conversation is parked.
     *      Must not be null. This dock is resonpsible for
     *      resuming this conversation.
     * @return
     *      This method returns when the conversation is resumed.
     *      the parameter given to {@link Dock#resume(Object)} will be
     *      returned.
     */
    public abstract <T> T suspend(Dock<T> dock);

    public abstract <T> T suspend(List<? extends Dock<? extends T>> docks);

    public abstract <T> T suspend(Dock<? extends T>... docks);

    /**
     * Returns the {@link Conversation} that the current thread is executing.
     */
    public static ConversationSPI getCurrentConversation() {
        return EngineImpl.getCurrentConversation();
    }
}
