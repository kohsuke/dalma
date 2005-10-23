package dalma.endpoints.irc;

import f00f.net.irc.martyr.commands.MessageCommand;

/**
 * An IRC user (and an implicit {@link Session} that represents
 * a private communication channel between this user.)
 *
 * @author Kohsuke Kawaguchi
 */
public class Buddy extends Session {
    /*package*/ String name;

    public Buddy(IRCEndPoint endpoint, String name) {
        super(endpoint);
        this.name = name;
    }

    /**
     * Gets the name of this user.
     * @return always non-null.
     */
    public String getName() {
        return name;
    }

    public boolean isOnline() {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
        // TODO: use ISON command
    }

    public void setMode(Object... modes) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
    
    public void send(String message) {
        endpoint.connection.sendCommand(new MessageCommand(name,message));
    }

    public String waitForNextMessage() {
        // TODO
        return super.waitForNextMessage();
    }
}
