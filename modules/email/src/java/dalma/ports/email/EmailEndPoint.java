package dalma.ports.email;

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
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link EndPoint} connected to an e-mail address.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmailEndPoint extends EndPointImpl {
    /**
     * Conversations waiting for a reply, keyed by their Message ID.
     */
    private static final Map<UUID,MailReceiver> queue =
        Collections.synchronizedMap(new HashMap<UUID,MailReceiver>());

    private static final Logger logger = Logger.getLogger(EmailEndPoint.class.getName());
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

    /*package*/ static void register(MailReceiver mr) {
        queue.put(mr.getUUID(),mr);
    }

    /*paclage*/ static void unregister(MailReceiver mr) {
        queue.remove(mr.getUUID());
    }

    /**
     * Invoked when a new message is received.
     */
    /*package*/ void handleMessage(MimeMessage msg) throws MessagingException {
        // see http://cr.yp.to/immhf/thread.html
        UUID id = getIdHeader(msg, "References");
        if(id==null)
            id = getIdHeader(msg,"In-reply-to");
        if(id==null) {
            NewMailHandler h = newMailHandler;
            if(h!=null) {
                try {
                    h.onNewMail(new MimeMessageEx(msg));
                } catch (Exception e) {
                    logger.log(Level.WARNING,"Unhandled exception",e);
                }
            }
            return;
        }
        MailReceiver receiver = queue.get(id);
        if(receiver==null) {
            throw new MessagingException(
                "No conversation is waiting for the message id="+id);
        }
        receiver.handleMessage(new MimeMessageEx(msg));
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

    protected static class DockImpl extends Dock<MimeMessage> implements MailReceiver {
        private final UUID uuid;

        /**
         * The out-going message to be sent.
         *
         * The field is transient because we'll send it before
         * the dock is serialized, and thereafter never be used.
         */
        private transient Sender sender;

        public DockImpl(EmailEndPoint port, MimeMessage outgoing) throws MessagingException {
            super(port);
            this.sender = new Sender(outgoing);
            this.uuid = sender.uuid;
        }

        public UUID getUUID() {
            return uuid;
        }

        public void handleMessage(MimeMessage msg) {
            unregister(this);
            resume(msg);
        }

        public void park() {
            register(this);
            if(sender!=null) {
                try {
                    sender.send();
                } finally {
                    sender = null;
                }
            }
        }

        public void interrupt() {
            unregister(this);
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
     * Sends an e-mail out and waits for multiple replies.
     */
    public Iterator<MimeMessage> waitForMultipleReplies(MimeMessage outgoing) {
        try {
            ReplyIterator r = new ReplyIterator(this,outgoing);
            ConversationSPI.getCurrentConversation().addGenerator(r);
            return r;
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
