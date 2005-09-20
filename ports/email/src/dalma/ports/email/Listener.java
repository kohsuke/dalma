package dalma.ports.email;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

/**
 * Listens to the incoming e-mail messages and pass it to
 * {@link EmailEndPoint}.
 *
 * <p>
 * Derived classes are expected to provide the actual implementation of the listeneing logic.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Listener {
    protected Listener() {}

    /**
     * Derived classes should call this method when
     * a new e-mail is received.
     *
     * This method can be invoked from any thread.
     */
    protected void handleMessage(MimeMessage msg) throws MessagingException {
        EmailEndPointImpl.handleMessage(msg);
    }
}
