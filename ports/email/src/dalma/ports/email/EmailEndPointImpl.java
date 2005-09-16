package dalma.ports.email;

import dalma.TimeUnit;
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
public abstract class EmailEndPointImpl implements EmailEndPoint {

    private static final Map<UUID,DockImpl> queue = new HashMap<UUID, DockImpl>();

    /**
     * The address that this endPoint is waiting.
     */
    private final Address address;

    protected EmailEndPointImpl(Address address) {
        this.address = address;
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
            outgoing.setHeader("Message-ID",uuid.toString());
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
            return (Message)ConversationSPI.getCurrentConversation().suspend(
                new DockImpl(this,wrapUp(outgoing)), TimerEndPoint.createDock(timeout,unit) );
        } catch (MessagingException e) {
            throw new EmailException(e);
        }
    }
}
