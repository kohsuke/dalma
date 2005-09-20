package dalma.ports.email;

import javax.mail.Store;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Flags;
import javax.mail.Session;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Picks up messages from POP3.
 *
 * @author Kohsuke Kawaguchi
 */
public class POP3Listener extends Listener {
    private final String host;
    private final String uid;
    private final String password;
    private final int interval;

    private static final Logger logger = Logger.getLogger(POP3Listener.class.getName());

    public POP3Listener(String host, String uid, String password, int interval) {
        this.host = host;
        this.uid = uid;
        this.password = password;
        this.interval = interval;

        Thread t = new Thread(new Runner());
        t.setDaemon(true);
        t.start();
    }

    private class Runner implements Runnable {
        public void run() {
            Session s = Session.getInstance(System.getProperties());
            while(true) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return; // treat as a signal to die
                }
                try {
                    logger.fine("connecting");
                    Store store = s.getStore("pop3");
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
