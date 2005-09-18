package dalma.ports.email;

import dalma.TimeUnit;
import dalma.spi.port.EndPoint;

import javax.mail.internet.MimeMessage;
import java.util.concurrent.TimeoutException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class EmailEndPoint extends EndPoint {
    protected EmailEndPoint(String name) {
        super(name);
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
    public abstract MimeMessage waitForReply(MimeMessage outgoing);

    /**
     * Sends a message and return immediately.
     *
     * Use this method when no further reply is expected.
     */
    public abstract void send(MimeMessage outgoing);


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
    public abstract MimeMessage waitForReply(MimeMessage outgoing,long timeout, TimeUnit unit) throws TimeoutException;

}
