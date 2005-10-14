package dalma.endpoints.jms;

import dalma.EndPoint;
import dalma.ReplyIterator;
import dalma.endpoints.jms.impl.BytesMessageImpl;
import dalma.endpoints.jms.impl.MapMessageImpl;
import dalma.endpoints.jms.impl.MessageImpl;
import dalma.endpoints.jms.impl.ObjectMessageImpl;
import dalma.endpoints.jms.impl.StreamMessageImpl;
import dalma.endpoints.jms.impl.TextMessageImpl;
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
import java.util.Date;
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

    protected void start() {
        // TODO: shall we control queue connection by ourselves, so that we can start it here?
    }

    protected void stop() {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new Error(e); // what else can we do?
        }
        try {
            sender.close();
        } catch (JMSException e) {
            throw new Error(e); // what else can we do?
        }
    }

    /**
     * Invoked by JMS.
     */
    public void onMessage(Message message) {
        try {
            super.handleMessage(wrap(message));
        } catch (JMSException e) {
            logger.log(Level.WARNING,"JMSEndPoint encountered an JMS error",e);
        }
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

    /**
     * Sends a message and returns immediately.
     */
    public String send(Message msg) {
        try {
            Message providerMsg = unwrap(msg);
            sender.send(providerMsg);
            if(msg instanceof MessageImpl) {
                // JMS sets various properties as a result of the send operation
                // propagate them back to the message
                ((MessageImpl)msg).wrap(providerMsg);
            }
            return providerMsg.getJMSMessageID();
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
    public <T extends Message> T createMessage(Class<T> type) {
        if(type==BytesMessage.class)
            return type.cast(new BytesMessageImpl());
        if(type==MapMessage.class)
            return type.cast(new MapMessageImpl());
        if(type==ObjectMessage.class)
            return type.cast(new ObjectMessageImpl());
        if(type==StreamMessage.class)
            return type.cast(new StreamMessageImpl());
        if(type==TextMessage.class)
            return type.cast(new TextMessageImpl());
        throw new IllegalArgumentException();
    }

    /**
     * Wraps a provider-specific JMS Message object into our serializable wrapper.
     */
    private <T extends Message> T wrap(T msg) throws JMSException {
        if(msg instanceof BytesMessage)
            return (T)new BytesMessageImpl().wrap((BytesMessage)msg);
        if(msg instanceof MapMessage)
            return (T)new MapMessageImpl().wrap((MapMessage)msg);
        if(msg instanceof ObjectMessage)
            return (T)new ObjectMessageImpl().wrap((ObjectMessage)msg);
        if(msg instanceof StreamMessage)
            return (T)new StreamMessageImpl().wrap((StreamMessage)msg);
        if(msg instanceof TextMessage)
            return (T)new TextMessageImpl().wrap((TextMessage)msg);
        throw new IllegalArgumentException();
    }

    /**
     * Unwraps our serializable wrapper into a provider-specific JMS Message.
     */
    private <T extends Message> T unwrap(T msg) throws JMSException {
        if(msg instanceof BytesMessageImpl) {
            BytesMessage r = session.createBytesMessage();
            ((BytesMessageImpl)msg).writeTo(r);
            return (T)r;
        }
        if(msg instanceof MapMessageImpl) {
            MapMessage r = session.createMapMessage();
            ((MapMessageImpl)msg).writeTo(r);
            return (T)r;
        }
        if(msg instanceof ObjectMessage) {
            ObjectMessage r = session.createObjectMessage();
            ((ObjectMessageImpl)msg).writeTo(r);
            return (T)r;
        }
        if(msg instanceof StreamMessage) {
            StreamMessage r = session.createStreamMessage();
            ((StreamMessageImpl)msg).writeTo(r);
            return (T)r;
        }
        if(msg instanceof TextMessage) {
            TextMessage r = session.createTextMessage();
            ((TextMessageImpl)msg).writeTo(r);
            return (T)r;
        }
        return msg;
    }

    /**
     * Creates a reply to the specified message.
     */
    public <T extends Message> T createReplyMessage(Class<T> type, Message in) throws JMSException {
        T reply = createMessage(type);
        reply.setJMSCorrelationID(in.getJMSMessageID());
        return reply;
    }

    public Message waitForReply(Message msg) {
        return super.waitForReply(msg);
    }

    public Message waitForReply(Message msg, Date timeout) {
        return super.waitForReply(msg, timeout);
    }

    public ReplyIterator<Message> waitForMultipleReplies(Message outgoing, Date expirationDate) {
        return super.waitForMultipleReplies(outgoing, expirationDate);
    }
}
