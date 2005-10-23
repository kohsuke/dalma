package dalma.endpoints.irc;

import dalma.impl.EndPointImpl;
import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.NickCommand;
import f00f.net.irc.martyr.commands.RawCommand;
import f00f.net.irc.martyr.services.AutoReconnect;
import f00f.net.irc.martyr.services.AutoRegister;
import f00f.net.irc.martyr.services.AutoResponder;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class IRCEndPoint extends EndPointImpl {
    /**
     * Active {@link Channel} instances keyed by their names.
     */
    // channel names are case insensitive, so all lower in this map
    private final Map<String,Channel> channels = new Hashtable<String,Channel>();

    /**
     * Active {@link Buddy} instances keyed by their names.
     */
    // nicknames are case insensitive, so all lower in this map
    private final Map<String,Buddy> buddies = new Hashtable<String, Buddy>();

    private static final Logger logger = Logger.getLogger(IRCEndPoint.class.getName());

    /*package*/ final IRCConnection connection = new IRCConnection();

    private final String ircServer;
    private final int port;

    public IRCEndPoint(String endpointName, String ircServer, int port) {
        super(endpointName);

        this.ircServer = ircServer;
        this.port = port;

        // AutoRegister and AutoResponder both add themselves to the
        // appropriate observerables.  Both will remove themselves with the
        // disable() method.
        new AutoRegister(connection, "repp", "bdamm", "Ben Damm");
        new AutoResponder(connection);
        connection.addCommandObserver(new MessageListener(this));
    }

    protected void start() {
        AutoReconnect autoRecon = new AutoReconnect(connection);
        autoRecon.go(ircServer,port);
    }

    protected void stop() {
        connection.disconnect();
    }

    /**
     * Sets the away status.
     */
    public void setAway(String status) {
        connection.sendCommand(new RawCommand("AWAY",status));
    }

    /**
     * Sets the new nick name.
     */
    public void setNick(String nickname) {
        connection.sendCommand(new NickCommand(nickname));
    }

    /**
     * Lists all the {@link Channel}s on the current server.
     */
    public Collection<Channel> listChannels() {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Called when a new message is received from IRC.
     */
    /*package*/ void onMessageReceived(MessageCommand cmd) {
        String dest = cmd.getDest().toLowerCase();

        // figure out the sender
        Buddy sender = getBuddy(cmd.getSource().getNick());

        // is this message for me persnoally?
        if(cmd.isPrivateToUs(connection.getClientState())) {
            Message msg = new Message(sender, cmd.getMessage(), null);

            // TODO: if someone is waiting for a message on a buddy
            // wake him up. otherwise call the listener
            throw new UnsupportedOperationException();
        }

        // otherwise it must be to a channel
        Channel channel = channels.get(dest);
        if(channel!=null) {
            channel.onMessageReceived(new Message(sender,cmd.getMessage(),channel));
            return;
        }

        // is this possible!?
        logger.log(Level.WARNING,"Unrecognized message: "+cmd.renderParams());
    }

    /**
     * Gets the {@link Buddy} object that represents the given nick name.
     *
     * <p>
     * This method succeds even if no such user exists.
     *
     * @param nickname
     *      The IRC nickname of the buddy, like "kohsuke".
     * @return
     *      always non-null.
     */
    public Buddy getBuddy(String nickname) {
        synchronized(buddies) {
            Buddy buddy = buddies.get(nickname);
            if(buddy==null) {
                buddy = new Buddy(this,nickname);
                buddies.put(nickname,buddy);
            }
            return buddy;
        }
    }
}
