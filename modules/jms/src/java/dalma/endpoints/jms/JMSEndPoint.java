package dalma.endpoints.jms;

import dalma.Dock;
import dalma.EndPoint;
import dalma.impl.EndPointImpl;
import dalma.spi.ConversationSPI;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueSession;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@link EndPoint} that connects to two JMS queues.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMSEndPoint extends EndPointImpl implements MessageListener {
    private final QueueSession session;

    private final MessageProducer sender;
    private final MessageConsumer consumer;

    /**
     * Conversations waiting for a reply, keyed by their correlation ID.
     */
    private static final Map<UUID,DockImpl> queue = new HashMap<UUID, DockImpl>();

    public JMSEndPoint(String name, QueueSession session, Destination out, Destination in) throws JMSException {
        super(name);
        this.session = session;
        sender =session.createProducer(out);
        consumer = session.createConsumer(in);
        consumer.setMessageListener(this);
    }

    /**
     * Sends/publishes a JMS message and blocks until a reply is received.
     */
    public Message waitForReply(Message message) throws JMSException {
        return ConversationSPI.getCurrentConversation().suspend(
            new DockImpl(this,message));
    }

    protected void stop() {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new Error(e); // what else can we do?
        }
    }

    /**
     * Creates a new blank {@link Message} of the specified type.
     *
     * @param type
     *      one of 5 {@link Message}-derived types defined in JMS.
     */
    public <T extends Message> T createMessage(Class<T> type) throws JMSException {
        if(type==BytesMessage.class)
            return (T)session.createBytesMessage();
        if(type==MapMessage.class)
            return (T)session.createMapMessage();
        if(type==ObjectMessage.class)
            return (T)session.createObjectMessage();
        if(type==StreamMessage.class)
            return (T)session.createStreamMessage();
        if(type==TextMessage.class)
            return (T)session.createTextMessage();
        throw new IllegalArgumentException();
    }

    /**
     * Receives incoming messages.
     */
    public void onMessage(Message message) {
        try {
            UUID id = UUID.fromString(message.getJMSCorrelationID());

            DockImpl dock;
            synchronized(queue) {
                dock = queue.remove(id);
            }
            if(dock==null) {
                throw new JMSException(
                    "No conversation is waiting for the message id="+id);
            }
            dock.resume(message);
        } catch (JMSException e) {
            // TODO
            throw new Error(e);
        }
    }


    protected static class DockImpl extends Dock<Message> {
        /**
         * Correlation ID.
         */
        private final UUID uuid;

        /**
         * The out-going message to be sent.
         * This needs to be sent out after this dock is placed,
         * so that we can safely ignore all incoming messages
         * that doesn't have docks waiting for it.
         *
         * The field is transient because once it's sent out
         * the field is kept to null.
         */
        private transient Message outgoing;

        public DockImpl(JMSEndPoint port, Message outgoing) throws JMSException {
            super(port);
            this.outgoing = outgoing;

            // this creates a cryptographically strong GUID,
            // meaning someone who knows any number of GUIDs can't
            // predict another one (to steal the session)
            uuid = UUID.randomUUID();
            outgoing.setJMSCorrelationID(uuid.toString());
        }

        public void park() {
            synchronized(queue) {
                queue.put(uuid,this);
            }
            if(outgoing!=null) {
                try {
                    ((JMSEndPoint)endPoint).sender.send(outgoing);
                } catch (JMSException e) {
                    // TODO
                    throw new Error(e);
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
}
