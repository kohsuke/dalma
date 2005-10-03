package dalma.endpoints.jms;

import dalma.EndPoint;
import dalma.spi.port.MultiplexedEndPoint;

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
import java.util.logging.Level;

/**
 * {@link EndPoint} that connects to two JMS queues.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMSEndPoint extends MultiplexedEndPoint<String,Message> implements MessageListener {
    private final QueueSession session;

    private final MessageProducer sender;
    private final MessageConsumer consumer;

    /**
     * Handles uncorrelated messages.
     */
    private MessageHandler newMessageHandler;

    public JMSEndPoint(String name, QueueSession session, Destination out, Destination in) throws JMSException {
        super(name);
        this.session = session;
        sender =session.createProducer(out);
        consumer = session.createConsumer(in);
        consumer.setMessageListener(this);
    }

    protected void stop() {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new Error(e); // what else can we do?
        }
    }

    /**
     * Invoked by JMS.
     */
    public void onMessage(Message message) {
        super.handleMessage(message);
    }

    protected String getKey(Message msg) {
        try {
            return msg.getJMSCorrelationID();
        } catch (JMSException e) {
            throw new QueueException(e);
        }
    }

    protected void onNewMessage(Message msg) {
        MessageHandler h = newMessageHandler;
        if(h !=null) {
            try {
                h.onNewMessage(msg);
            } catch (Exception e) {
                logger.log(Level.WARNING,e.getMessage(),e);
            }
        }
    }

    protected String send(Message msg) {
        try {
            sender.send(msg);
            return msg.getJMSCorrelationID();
        } catch (JMSException e) {
            throw new QueueException(e);
        }
    }

//
//
// API methods
//
//

    /**
     * Gets the last value set by {@link #setNewMessageHandler(MessageHandler)}.
     */
    public MessageHandler getNewMessageHandler() {
        return newMessageHandler;
    }

    /**
     * Sets {@link MessageHandler} that handles uncorrelated messages
     * received by this endpoint.
     *
     * @param newMessageHandler
     *      if null, uncorrelated messages are discarded.
     */
    public void setNewMessageHandler(MessageHandler newMessageHandler) {
        this.newMessageHandler = newMessageHandler;
    }

    /**
     * Creates a new blank {@link Message} of the specified type.
     *
     * @param type
     *      one of 5 {@link Message}-derived types defined in JMS.
     */
    public <T extends Message> T createMessage(Class<T> type) throws JMSException {
        if(type==BytesMessage.class)
            return type.cast(session.createBytesMessage());
        if(type==MapMessage.class)
            return type.cast(session.createMapMessage());
        if(type==ObjectMessage.class)
            return type.cast(session.createObjectMessage());
        if(type==StreamMessage.class)
            return type.cast(session.createStreamMessage());
        if(type==TextMessage.class)
            return type.cast(session.createTextMessage());
        throw new IllegalArgumentException();
    }

    /**
     * Creates a reply to the specified message.
     */
    public <T extends Message> T createReplyMessage(Class<T> type, Message in) throws JMSException {
        T reply = createMessage(type);
        reply.setJMSCorrelationID(in.getJMSMessageID());
        return reply;
    }

    /**
     * Sends/publishes a JMS message and blocks until a reply is received.
     */
    public Message waitForReply(Message message) {
        return super.waitForReply(message);
    }
}
