package dalma.endpoints.irc;

import f00f.net.irc.martyr.commands.JoinCommand;
import f00f.net.irc.martyr.commands.PartCommand;
import f00f.net.irc.martyr.commands.InviteCommand;
import f00f.net.irc.martyr.commands.KickCommand;
import f00f.net.irc.martyr.commands.RawCommand;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.clientstate.*;

import java.util.Collection;
import java.util.List;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Represents a channel in IRC.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Channel extends Session {
    private final String name;

    /**
     * Messages that are received.
     */
    private final List<Message> msgs = Collections.synchronizedList(new LinkedList<Message>());

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

    public void join() {
        endpoint.connection.sendCommand(new JoinCommand(name));
    }

    public void join(String secret) {
        endpoint.connection.sendCommand(new JoinCommand(name,secret));
    }

    public void part() {
        endpoint.connection.sendCommand(new PartCommand(name));
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
     * Invoked when a new message is received from IRC.
     */
    protected void onMessageReceived(Message msg) {
        msgs.add(msg);
        // TODO: wake up the blocking conversation if any
    }
}
