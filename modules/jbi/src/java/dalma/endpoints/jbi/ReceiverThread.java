package dalma.endpoints.jbi;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Kohsuke Kawaguchi
 */
final class ReceiverThread extends Thread {
    private final JBIEndPoint endPoint;
    private final DeliveryChannel channel;

    private static final Logger logger = Logger.getLogger(ReceiverThread.class.getName());

    public ReceiverThread(JBIEndPoint endPoint, DeliveryChannel channel) {
        this.endPoint = endPoint;
        this.channel = channel;
    }

    public void run() {
        while(true) {
            try {
                MessageExchange me = channel.accept();
                endPoint.onNewMessage(me);
            } catch (MessagingException e) {
                logger.log(Level.WARNING,"Failed to accept a message via JBI",e);
            }
        }
    }
}
