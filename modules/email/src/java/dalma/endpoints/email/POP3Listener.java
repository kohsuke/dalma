package dalma.endpoints.email;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Listener} that picks up messages from a POP3 server.
 *
 * <p>
 * This {@link Listener} periodically connects to a POP3 server
 * and retrieves e-mails from there. The basic connection parameters
 * are configured through the constructor, and the rest can be
 * controlled through JavaMail properties on {@link EmailEndPoint}
 * (see {@link EmailEndPoint#getSession()}.)
 *
 * <p>
 * Once retrieved, e-mails are deleted from the server, regardless
 * of whether the processing of the e-mail succeeded or not (Otherwise
 * this listener will end up reading the same message over and over.)
 *
 * @author Kohsuke Kawaguchi
 */
public class POP3Listener extends Listener {
    private final String host;
    private final String uid;
    private final String password;
    private final int interval;
    private final Thread thread;

    private static final Logger logger = Logger.getLogger(POP3Listener.class.getName());

    /**
     * Creates a new {@link POP3Listener}.
     *
     * @param host
     *      Name of the POP3 server, such as "mail.acme.org"
     *      must not be null.
     * @param uid
     *      The user name used to log in to the POP3 server.
     * @param password
     *      The password used to log in to the POP3 server.
     * @param interval
     *      The polling interval in milliseconds.
     */
    public POP3Listener(String host, String uid, String password, int interval) {
        this.host = host;
        this.uid = uid;
        this.password = password;
        this.interval = interval;

        thread = new Thread(new Runner(),"POP3 listener thread for "+uid+'@'+host);
    }

    protected void setEndPoint(EmailEndPoint ep) {
        super.setEndPoint(ep);
        thread.setDaemon(true);
    }

    protected void start() {
        thread.start();
    }

    protected void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // process this interruption later
            Thread.currentThread().interrupt();
        }
    }

    private class Runner implements Runnable {
        public void run() {
            while(true) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return; // treat as a signal to die
                }
                try {
                    logger.fine("connecting");
                    Store store = getEndPoint().getSession().getStore("pop3");
                    store.connect(host,uid,password);
                    logger.fine("connected");

                    Folder folder = store.getFolder("INBOX");
                    folder.open(Folder.READ_WRITE);
                    Message[] msgs = folder.getMessages();
                    for( Message msg : msgs ) {
                        logger.fine("handling message: "+msg.getSubject());
                        msg.setFlag(Flags.Flag.DELETED,true);
                        try {
                            handleMessage((MimeMessage)msg);
                        } catch (MessagingException e) {
                            logger.log(Level.WARNING,"failed to handle message: "+msg.getSubject(),e);
                            // but delete this message anyway
                        }
                    }
                    folder.close(true);
                    logger.fine("done. going back to sleep");
                } catch (MessagingException e) {
                    logger.log(Level.WARNING,"failed to connect to the POP3 server",e);
                }
            }
        }
    }
}
