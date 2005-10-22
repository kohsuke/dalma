package dalma.endpoints.jbi;

import dalma.spi.port.MultiplexedEndPoint;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import java.util.logging.Level;

/**
 * @author Kohsuke Kawaguchi
 */
public class JBIEndPoint extends MultiplexedEndPoint<String, MessageExchange> {

    private final DeliveryChannel channel;

    private MessageExchangeHandler meHandler;

    private Thread receiverThread;

    public JBIEndPoint(String name, DeliveryChannel channel) {
        super(name);
        this.channel = channel;
        this.receiverThread = new ReceiverThread(this,channel);
    }

    protected String getKey(MessageExchange msg) {
        return msg.getExchangeId();
    }

    protected void onNewMessage(MessageExchange msg) {
        MessageExchangeHandler h = meHandler;
        if(h !=null) {
            try {
                h.onNewMessage(msg);
            } catch (Exception e) {
                logger.log(Level.WARNING,e.getMessage(),e);
            }
        }
    }

    protected String send(MessageExchange msg) {
        try {
            channel.send(msg);
            return msg.getExchangeId();
        } catch (MessagingException e) {
            throw new JBIException(e);
        }
    }

    protected void start() {
        receiverThread.start();
    }

    protected void stop() {
        receiverThread.interrupt();
        try {
            receiverThread.join();
        } catch (InterruptedException e) {
            // process the interruption later
            Thread.currentThread().interrupt();
        }
        try {
            channel.close();
        } catch (MessagingException e) {
            throw new JBIException(e);
        }
    }

    public MessageExchangeHandler getMessageExchangeHandler() {
        return meHandler;
    }

    public void setMessageExchangeHandler(MessageExchangeHandler meHandler) {
        this.meHandler = meHandler;
    }
}
