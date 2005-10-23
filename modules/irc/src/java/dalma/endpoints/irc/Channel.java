package dalma.endpoints.irc;

import f00f.net.irc.martyr.clientstate.Member;
import f00f.net.irc.martyr.commands.InviteCommand;
import f00f.net.irc.martyr.commands.JoinCommand;
import f00f.net.irc.martyr.commands.KickCommand;
import f00f.net.irc.martyr.commands.PartCommand;
import f00f.net.irc.martyr.commands.RawCommand;
import f00f.net.irc.martyr.commands.MessageCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.io.Serializable;

/**
 * Represents a channel in IRC.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Channel extends Session {
    private final String name;

    /**
     * True if the endpoint is a member of this channel.
     */
    private boolean joined;


    public Channel(IRCEndPoint endpoint,String name) {
        super(endpoint);
        this.name = name;
    }

    /**
     * Gets the channel name, such as "#dalma"
     *
     * @return never null.
     */
    public String getName() {
        return name;
    }

    /**
     * Join this channel.
     *
     * @throws IllegalStateException
     *      if we've already joined this channel.
     */
    public synchronized void join() {
        makeSureNotJoined();
        endpoint.connection.sendCommand(new JoinCommand(name));
        joined = true;
    }

    /**
     * Join this channel.
     *
     * @throws IllegalStateException
     *      if we've already joined this channel.
     */
    public synchronized void join(String secret) {
        makeSureNotJoined();
        endpoint.connection.sendCommand(new JoinCommand(name,secret));
        joined = true;
    }

    private synchronized void makeSureNotJoined() {
        if(joined)
            throw new IllegalStateException("already joined");
    }

    /**
     * Invites the specified user to this channel.
     */
    public void invite(Buddy buddy) {
        endpoint.connection.sendCommand(new InviteCommand(buddy.name,name));
    }

    /**
     * Kicks out the specified user from this channel.
     */
    public void kick(Buddy buddy,String comment) {
        endpoint.connection.sendCommand(new KickCommand(name,buddy.name,comment));
    }

    public void knock(String msg) {
        makeSureNotJoined();
        endpoint.connection.sendCommand(new RawCommand(name,msg));
    }

    /**
     * Gets all the people on this channel.
     */
    public Collection<Buddy> getMembers() {
        List<Buddy> buddies = new ArrayList<Buddy>();
        Enumeration members = getChannelInfo().getMembers();
        while (members.hasMoreElements()) {
            Member m = (Member) members.nextElement();
            Buddy b = endpoint.getBuddy(m.getNick().getNick());
            buddies.add(b);
        }
        return buddies;
    }

    /**
     * Gets the object that represents information about this channel
     * from martyr.
     */
    private f00f.net.irc.martyr.clientstate.Channel getChannelInfo() {
        return endpoint.connection.getClientState().getChannel(name);
    }

    public String getTopic() {
        return getChannelInfo().getTopic();
    }

    public void setTopic(String newTopic) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    public void setMode(Object... modes) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Sends a message to this {@link Channel}.
     */
    public void send(String message) {
        endpoint.connection.sendCommand(new MessageCommand(name,message));
    }

    /**
     * Sends the 'PART' message and leaves from this channel.
     */
    public synchronized void close() {
        endpoint.connection.sendCommand(new PartCommand(name));
        joined = false;
    }

    protected Object writeReplace() {
        return new Moniker(endpoint,name);
    }

    private static final class Moniker implements Serializable {
        private final IRCEndPoint endPoint;
        private final String channelName;

        public Moniker(IRCEndPoint endPoint, String channelName) {
            this.endPoint = endPoint;
            this.channelName = channelName;
        }

        private Object readResolve() {
            return endPoint.getChannel(channelName);
        }

        private static final long serialVersionUID = 1L;
    }
}
