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
 * {@link Listener} that picks up messages from an IMAP4 server.
 * 
 * @author Kohsuke Kawaguchi
 */
public class IMAP4Listener extends Listener {
    private final String host;
    private final String uid;
    private final String password;
    private final int interval;
    private final Thread thread;

    private static final Logger logger = Logger.getLogger(POP3Listener.class.getName());

    public IMAP4Listener(String host, String uid, String password, int interval) {
        this.host = host;
        this.uid = uid;
        this.password = password;
        this.interval = interval;
        this.thread = new Thread(new Runner(),"IMAP4 listener thread for "+uid+'@'+host);
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
                    Store store = getEndPoint().getSession().getStore("imap");
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
