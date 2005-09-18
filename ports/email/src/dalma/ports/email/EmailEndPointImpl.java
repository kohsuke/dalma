package dalma.ports.email;

import dalma.TimeUnit;
import dalma.Conversation;
import dalma.spi.ConversationSPI;
import dalma.spi.port.Dock;
import test.port.timer.TimerEndPoint;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Kohsuke Kawaguchi
 */
public class EmailEndPointImpl extends EmailEndPoint {

    private static final Map<UUID,DockImpl> queue = new HashMap<UUID, DockImpl>();

    /**
     * The address that receives replies.
     */
    private final Address address;

    public EmailEndPointImpl(String name, Address address) {
        super(name);
        this.address = address;
    }

    /**
     * Invoked when a new message is received to awake the corresponding
     * {@link Conversation}.
     */
    /*package*/ static void handleMessage(Message msg) throws MessagingException {
        // see http://cr.yp.to/immhf/thread.html
        UUID id = getIdHeader(msg, "References");
        if(id==null)
            id = getIdHeader(msg,"In-reply-to");
        if(id==null) {
            throw new MessagingException(
                "Neither In-reply-to nor References header was found.\n" +
                "Unable to link this message to a conversation");
        }
        DockImpl dock;
        synchronized(queue) {
            dock = queue.remove(id);
        }
        if(dock==null) {
            throw new MessagingException(
                "No conversation is waiting for the message id="+id);
        }
        dock.resume(msg);
    }

    private static UUID getIdHeader(Message msg, String name) throws MessagingException {
        String[] h = msg.getHeader(name);
        if(h==null || h.length==0)
            return null;

        String val = h[0].trim();
        if(val.length()<2)  return null;

        // find the last token, if there are more than one
        int idx = val.lastIndexOf(' ');
        if(idx>0)   val = val.substring(idx+1);

        if(!val.startsWith("<"))    return null;
        val = val.substring(1);
        if(!val.endsWith("@localhost>"))    return null;
        val = val.substring(0,val.length()-"@localhost>".length());

        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;    // not a UUID
        }
    }

    protected static class DockImpl extends Dock<Message> {
        private final UUID uuid;

        /**
         * The out-going message to be sent.
         * This needs to be sent out after this dock is placed,
         * so that we can safely ignore all incoming e-mails
         * that doesn't have docks waiting for it.
         *
         * The field is transient because once it's sent out
         * the field is kept to null.
         */
        private transient Message outgoing;

        public DockImpl(EmailEndPointImpl port, Message outgoing) throws MessagingException {
            super(port);
            this.outgoing = outgoing;

            // this creates a cryptographically strong GUID,
            // meaning someone who knows any number of GUIDs can't
            // predict another one (to steal the session)
            uuid = UUID.randomUUID();
            outgoing.setHeader("Message-ID",'<'+uuid.toString()+"@localhost>");
        }

        public void park() {
            synchronized(queue) {
                queue.put(uuid,this);
            }
            if(outgoing!=null) {
                try {
                    Transport.send(outgoing);
                } catch (MessagingException e) {
                    throw new EmailException(e);
                } finally {
                    outgoing = null;
                }
            }
        }

        public void interrupt() {
            synchronized(queue) {
                queue.remove(uuid);
            }
        }

        public void onLoad() {
            park();
        }
    }



    /**
     * Wraps up the out-going message.
     */
    private Message wrapUp(Message outgoing) throws MessagingException {
        outgoing.setReplyTo(new Address[]{address});
        if(outgoing.getFrom()==null || outgoing.getFrom().length==0) {
            outgoing.setFrom(address);
        }
        return outgoing;
    }


    public Message waitForReply(Message outgoing) {
        try {
            return ConversationSPI.getCurrentConversation().suspend(new DockImpl(this,wrapUp(outgoing)));
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }

    public Message waitForReply(Message outgoing, long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return ConversationSPI.getCurrentConversation().suspend(
                new DockImpl(this,wrapUp(outgoing)), TimerEndPoint.<Message>createDock(timeout,unit) );
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }
}
