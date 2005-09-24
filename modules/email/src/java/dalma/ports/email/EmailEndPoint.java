package dalma.ports.email;

import dalma.Conversation;
import dalma.Dock;
import dalma.EndPoint;
import dalma.TimeUnit;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.impl.EndPointImpl;
import dalma.spi.ConversationSPI;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * {@link EndPoint} connected to an e-mail address.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmailEndPoint extends EndPointImpl {
    /**
     * Conversations waiting for a reply, keyed by their Message ID.
     */
    private static final Map<UUID,DockImpl> queue = new HashMap<UUID, DockImpl>();

    /**
     * The address that receives replies.
     */
    private final Address address;

    private final Listener listener;

    private NewMailHandler newMailHandler;

    /**
     * Creates a new e-mail end point.
     *
     * @param name
     *      The unique name assigned by the application that identifies this endpoint.
     * @param address
     *      The e-mail address of this endpoint.
     * @param listener
     *      The object that fetches incoming e-mails.
     */
    public EmailEndPoint(String name, Address address, Listener listener) {
        super(name);
        this.address = address;
        this.listener = listener;
        listener.setEndPoint(this);
    }

    protected void stop() {
        listener.stop();
    }

    /**
     * Gets the listener set by {@link #setNewMailHandler(NewMailHandler)}.
     */
    public NewMailHandler getNewMailHandler() {
        return newMailHandler;
    }

    /**
     * Sets the handler that receives uncorrelated incoming e-mails.
     *
     * @see NewMailHandler
     */
    public void setNewMailHandler(NewMailHandler newMailHandler) {
        this.newMailHandler = newMailHandler;
    }

    /**
     * Invoked when a new message is received to awake the corresponding
     * {@link Conversation}.
     */
    /*package*/ void handleMessage(MimeMessage msg) throws MessagingException {
        // see http://cr.yp.to/immhf/thread.html
        UUID id = getIdHeader(msg, "References");
        if(id==null)
            id = getIdHeader(msg,"In-reply-to");
        if(id==null) {
            NewMailHandler h = newMailHandler;
            if(h!=null)
                h.onNewMail(msg);
            return;
        }
        DockImpl dock;
        synchronized(queue) {
            dock = queue.remove(id);
        }
        if(dock==null) {
            throw new MessagingException(
                "No conversation is waiting for the message id="+id);
        }
        dock.resume(new MimeMessageEx(msg));
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

    protected static class DockImpl extends Dock<MimeMessage> {
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
        private transient MimeMessage outgoing;

        public DockImpl(EmailEndPoint port, MimeMessage outgoing) throws MessagingException {
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
    private MimeMessage wrapUp(MimeMessage outgoing) throws MessagingException {
        outgoing.setReplyTo(new Address[]{address});
        if(outgoing.getFrom()==null || outgoing.getFrom().length==0) {
            outgoing.setFrom(address);
        }
        return outgoing;
    }

    /**
     * Sends an e-mail out and waits for a reply to come back.
     *
     * <p>
     * This method blocks forever until a reply is received.
     *
     * @param outgoing
     *      The message to be sent. Must not be null.
     * @return
     *      a message that represents the received reply.
     *      always a non-null valid message.
     */
    public MimeMessage waitForReply(MimeMessage outgoing) {
        try {
            return ConversationSPI.getCurrentConversation().suspend(new DockImpl(this,wrapUp(outgoing)));
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }

    /**
     * Sends a message and return immediately.
     *
     * Use this method when no further reply is expected.
     */
    public void send(MimeMessage outgoing) {
        try {
            Transport.send(wrapUp(outgoing));
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }


    /**
     * Sends an e-mail out and waits for a reply to come back,
     * at most the time specfied.
     *
     * @throws TimeoutException
     *      if a response was not received within the specified timeout period.
     * @param outgoing
     *      The message to be sent. Must not be null.
     * @return
     *      a message that represents the received reply.
     *      always a non-null valid message.
     */
    public MimeMessage waitForReply(MimeMessage outgoing,long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return ConversationSPI.getCurrentConversation().suspend(
                new DockImpl(this,wrapUp(outgoing)), TimerEndPoint.<MimeMessage>createDock(timeout,unit) );
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }

}
