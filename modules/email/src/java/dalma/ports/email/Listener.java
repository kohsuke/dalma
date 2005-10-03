package dalma.ports.email;

import dalma.impl.EndPointImpl;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

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
     * {@link EmailEndPoint} associated to this listener.
     */
    private EmailEndPoint endPoint;

    /**
     * Invoked when a {@link Listener} is added to {@link EmailEndPoint}.
     */
    protected void setEndPoint(EmailEndPoint ep) {
        if(this.endPoint!=null)
            throw new IllegalStateException("this listener is already registered with an endpoint");
        this.endPoint = ep;
    }

    /**
     * Gets the {@link EmailEndPoint} associated with this {@link Listener}.
     *
     * @return
     *      null if no association is made yet.
     */
    public EmailEndPoint getEndPoint() {
        return endPoint;
    }

    /**
     * Derived classes should call this method when
     * a new e-mail is received.
     *
     * This method can be invoked from any thread.
     */
    protected void handleMessage(MimeMessage msg) throws MessagingException {
        endPoint.handleMessage(msg);
    }

    /**
     * @see EndPointImpl#stop()
     */
    protected abstract void stop();
}
