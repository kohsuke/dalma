package dalma.impl;

import dalma.Conversation;
import dalma.spi.port.Dock;

/**
 * {@link Dock} that waits for the completion of a {@link Conversation}.
 *
 * @author Kohsuke Kawaguchi
 */
final class ConversationDock extends Dock<Conversation> {

    private final ConversationImpl conv;

    ConversationDock(ConversationImpl conv) {
        super(null);
        this.conv = conv;
    }

    public void park() {
        conv.waitList.add(this);
    }

    public void interrupt() {
        conv.waitList.remove(this);
    }

    public void onLoad() {
        park();
    }

    private static final long serialVersionUID = 1L;
}
