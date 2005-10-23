package dalma.endpoints.irc;

import dalma.impl.EndPointImpl;
import f00f.net.irc.martyr.IRCConnection;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.NickCommand;
import f00f.net.irc.martyr.commands.RawCommand;
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
    private final Map<String,Buddy> buddies = new Hashtable<String, Buddy>();

    private static final Logger logger = Logger.getLogger(IRCEndPoint.class.getName());

    /*package*/ final IRCConnection connection = new IRCConnection();

    private NewSessionListener newSessionListener;

    private final AutoReconnectEx autoReconnect;

    public IRCEndPoint(String endpointName, String ircServer, int port, String nickname) {
        super(endpointName);

        // AutoRegister and AutoResponder both add themselves to the
        // appropriate observerables.  Both will remove themselves with the
        // disable() method.
        new AutoRegister(connection, nickname, nickname, nickname);
        new AutoResponder(connection);
        autoReconnect = new AutoReconnectEx(connection,ircServer,port);
        connection.addCommandObserver(new MessageListener(this));
    }

    protected void start() {
        autoReconnect.go();
    }

    protected void stop() {
        connection.disconnect();
    }

    public NewSessionListener getNewSessionListener() {
        return newSessionListener;
    }

    public void setNewSessionListener(NewSessionListener newSessionListener) {
        this.newSessionListener = newSessionListener;
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

            PrivateChat chat = sender.getChat();
            if(chat!=null) {
                // route the message
                chat.onMessageReceived(msg);
                return;
            } else {
                // no chat session is going on with this user.
                NewSessionListener sl = newSessionListener;
                if(sl!=null) {
                    // start a new chat session and let the handler know
                    chat = sender.openChat();
                    chat.onMessageReceived(msg);
                    sl.onNewPrivateChat(chat);
                    return;
                } else {
                    // nobody seems to be interested in talking to you, sorry.
                    // just ignore the message
                    return;
                }
            }
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
