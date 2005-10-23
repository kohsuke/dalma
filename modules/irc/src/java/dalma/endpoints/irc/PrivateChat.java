package dalma.endpoints.irc;

import f00f.net.irc.martyr.commands.MessageCommand;

import java.io.Serializable;

/**
 * Represents a private communication channel between another user.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PrivateChat extends Session {

    private final Buddy buddy;

    PrivateChat(IRCEndPoint endpoint, Buddy buddy) {
        super(endpoint);
        this.buddy = buddy;
    }

    /**
     * Gets the {@link Buddy} to which this chat is held.
     *
     * @return never null.
     */
    public Buddy getBuddy() {
        return buddy;
    }

    public void send(String message) {
        endpoint.connection.sendCommand(new MessageCommand(buddy.getName(),message));
    }

    public void close() {
        buddy.onChatClosed();
    }

    protected Object writeReplace() {
        return new Moniker(buddy);
    }

    private static final class Moniker implements Serializable {
        private final Buddy buddy;

        public Moniker(Buddy buddy) {
            this.buddy = buddy;
        }

        private Object readResolve() {
            return buddy.getChat();
        }

        private static final long serialVersionUID = 1L;
    }
}
