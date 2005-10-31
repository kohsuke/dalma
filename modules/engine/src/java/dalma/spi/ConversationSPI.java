package dalma.spi;

import dalma.Conversation;
import dalma.EndPoint;
import dalma.impl.ConversationImpl;
import dalma.impl.GeneratorImpl;

/**
 * Additional methods of {@link Conversation} available for {@link EndPoint}s.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ConversationSPI implements Conversation {

    /**
     * Returns the {@link Conversation} that the current thread is executing.
     */
    public static ConversationSPI currentConversation() {
        return ConversationImpl.currentConversation();
    }

    /**
     * Gets the engine to which this conversation belongs to.
     */
    public abstract EngineSPI getEngine();

    public abstract void addGenerator(GeneratorImpl generator);
}
