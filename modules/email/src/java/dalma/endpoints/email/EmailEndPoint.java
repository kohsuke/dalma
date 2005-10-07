package dalma.endpoints.email;

import dalma.EndPoint;
import dalma.ReplyIterator;
import dalma.TimeUnit;
import dalma.spi.port.MultiplexedEndPoint;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * {@link EndPoint} connected to an e-mail address.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmailEndPoint extends MultiplexedEndPoint<UUID,MimeMessage> {

    /**
     * The address that receives replies.
     */
    private final InternetAddress address;

    private final Listener listener;

    private NewMailHandler newMailHandler;

    /**
     * The JavaMail configuration.
     */
    private final Session session;

    /**
     * Creates a new e-mail end point.
     *
     * <p>
     * This uses {@code Session.getInstance(System.getProperties())} as the JavaMail session,
     * so effectively it configures JavaMail from the system properties.
     *
     * @param name
     *      The unique name assigned by the application that identifies this endpoint.
     * @param address
     *      The e-mail address of this endpoint.
     * @param listener
     *      The object that fetches incoming e-mails.
     */
    public EmailEndPoint(String name, InternetAddress address, Listener listener) {
        this(name,address,listener,Session.getInstance(System.getProperties()));
    }

    /**
     * Creates a new e-mail end point.
     *
     * <p>
     * This version takes the address as string so that it can be invoked from Spring.
     * It's just a short-cut for:
     * <pre>
     * this(name,new InternetAddress(address),listener,Session.getInstance(System.getProperties()))
     * </pre>
     * @see #EmailEndPoint(String, InternetAddress, Listener)
     */
    public EmailEndPoint(String name, String address, Listener listener) throws AddressException {
        this(name,new InternetAddress(address),listener,Session.getInstance(System.getProperties()));
    }

    /**
     * Creates a new e-mail end point.
     *
     * @param name
     *      The unique name assigned by the application that identifies this endpoint.
     * @param address
     *      The e-mail address of this endpoint.
     * @param listener
     *      The object that fetches incoming e-mails.
     * @param session
     *      The JavaMail configuration.
     */
    public EmailEndPoint(String name, InternetAddress address, Listener listener, Session session) {
        super(name);
        this.address = address;
        this.listener = listener;
        this.session = session;
        if(address==null || listener==null || session==null)
            throw new IllegalArgumentException();
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
     * Gets the JavaMail session that this endpoint uses to configure
     * JavaMail.
     *
     * @return
     *      always non-null.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the e-mail address that this endpoint is connected to.
     */
    public InternetAddress getAddress() {
        return address;
    }

    protected UUID getKey(MimeMessage msg) {
        try {
            UUID id = getIdHeader(msg, "References");
            if(id!=null)    return id;

            return getIdHeader(msg,"In-reply-to");
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }


    protected void onNewMessage(MimeMessage msg) {
        NewMailHandler h = newMailHandler;
        if(h!=null) {
            try {
                h.onNewMail(new MimeMessageEx(msg));
            } catch (Exception e) {
                logger.log(Level.WARNING,"Unhandled exception",e);
            }
        }
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

    protected void handleMessage(MimeMessage msg) {
        try {
            super.handleMessage(new MimeMessageEx(msg));
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }

    /**
     * Sends a message and return immediately.
     *
     * Use this method when no further reply is expected.
     */
    public UUID send(MimeMessage msg) {
        try {
            msg.setReplyTo(new Address[]{address});
            if(msg.getFrom()==null || msg.getFrom().length==0) {
                msg.setFrom(address);
            }

            // this creates a cryptographically strong GUID,
            // meaning someone who knows any number of GUIDs can't
            // predict another one (to steal the session)
            UUID uuid = UUID.randomUUID();
            msg.setHeader("Message-ID",'<'+uuid.toString()+"@localhost>");

            Transport.send(msg);

            return uuid;
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
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
        return super.waitForReply(outgoing);
    }

    /**
     * Sends an e-mail out and waits for multiple replies.
     *
     * <p>
     * Upon a successful completion, this method returns an {@link ReplyIterator}
     * that receives replies to the e-mail that was just sent, until the specified
     * expiration date is reached.
     *
     * @param outgoing
     *      The message to be sent. Must not be null.
     * @param expirationDate
     *      null to indicate that the iterator shall never be expired.
     *      Otherwise, the iterator will stop accepting reply messages
     *      once the specified date is reached.
     * @return
     *      always non-null.
     * @see ReplyIterator
     */
    public ReplyIterator<MimeMessage> waitForMultipleReplies(MimeMessage outgoing, Date expirationDate ) {
        return super.waitForMultipleReplies(outgoing,expirationDate);
    }

    /**
     * Sends an e-mail out and waits for multiple replies.
     *
     * <p>
     * The timeout and unit parameters together specifies the time period
     * in which the returned iterator waits for replies. For example,
     * if you set "1 week", the returned iterator will catch all replies received
     * within 1 week from now. See {@link #waitForMultipleReplies(MimeMessage, long, TimeUnit)} for
     * details.
     */
    public ReplyIterator<MimeMessage> waitForMultipleReplies(MimeMessage outgoing, long timeout, TimeUnit unit ) {
        return waitForMultipleReplies(outgoing,unit.fromNow(timeout));
    }

    /**
     * Sends an e-mail out and waits for multiple replies.
     *
     * <p>
     * This overloaded version returns a {@link ReplyIterator} that never expires.
     * See {@link #waitForMultipleReplies(MimeMessage, long, TimeUnit)} for details.
     */
    public ReplyIterator<MimeMessage> waitForMultipleReplies(MimeMessage outgoing) {
        return waitForMultipleReplies(outgoing,null);
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
        return super.waitForReply(outgoing, unit.fromNow(timeout));
    }
}
