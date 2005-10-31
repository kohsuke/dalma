package dalma.impl;

import dalma.Condition;
import dalma.Conversation;

/**
 * {@link Condition} that waits for the completion of a {@link Conversation}.
 *
 * @author Kohsuke Kawaguchi
 */
final class ConversationCondition extends Condition<Conversation> {

    private final ConversationImpl conv;

    ConversationCondition(ConversationImpl conv) {
        this.conv = conv;
    }

    public void onParked() {
        conv.waitList.add(this);
    }

    public void interrupt() {
        conv.waitList.remove(this);
    }

    public void onLoad() {
        onParked();
    }

    private static final long serialVersionUID = 1L;
}
