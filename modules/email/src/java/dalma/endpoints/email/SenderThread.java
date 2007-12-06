package dalma.endpoints.email;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Thread} that send e-mails via SMTP.
 *
 * <p>
 * This mechanism allows multiple e-mails to be sent through one connection,
 * and thereby improve the performance.
 *
 * @author Kohsuke Kawaguchi
 */
final class SenderThread extends Thread {
    private static final class Unit {
        final MimeMessage msg;
        final Throwable sender;

        private Unit(MimeMessage msg) {
            this.msg = msg;
            sender = new Exception().fillInStackTrace();
        }
    }

    private final List<Unit> msgs = new ArrayList<Unit>();

    private final Session session;

    private boolean isShuttingDown = false;

    private static final Logger logger = Logger.getLogger(SenderThread.class.getName());

    public SenderThread(Session session) {
        super("SMTP sender thread");
        this.session = session;
    }

    public synchronized void queue(MimeMessage msg) {
        if(isShuttingDown)
            throw new IllegalStateException("the sender thread is shutting down");

        msgs.add(new Unit(msg));
        notify();
    }

    private synchronized void waitForMessage() throws InterruptedException {
        while(msgs.isEmpty())
            this.wait();
    }

    public synchronized void shutDown() {
        if(!isShuttingDown) {
            isShuttingDown = true;
            interrupt();
        }
        try {
            join();
        } catch (InterruptedException e) {
            // process an interrupt later
            Thread.currentThread().interrupt();
        }
    }

    private synchronized boolean hasMessage() {
        return !msgs.isEmpty();
    }

    private synchronized Unit getNext() {
        return msgs.remove(0);
    }

    public void run() {
        while(!isShuttingDown) {
            try {
                waitForMessage();
            } catch (InterruptedException e) {
                // going to shutdown
                assert isShuttingDown;
                if(!hasMessage())
                    return;
            }

            // send all the messages in the queue
            try {
                logger.fine(toString()+" : waking up");
                Transport t = session.getTransport("smtp");
                t.connect();
                do {
                    Unit unit = getNext();
                    MimeMessage msg = unit.msg;
                    logger.fine(toString()+" : sending "+msg.getSubject());
                    try {
                        t.sendMessage(msg,msg.getAllRecipients());
                    } catch (MessagingException e) {
                        logger.log(Level.WARNING,"Failed to send an e-mail via SMTP",e);
                        logger.log(Level.WARNING,"Message created here",e);
                    }
                } while(hasMessage());
                t.close();
                logger.fine(toString()+" : going back to sleep");
            } catch (MessagingException e) {
                logger.log(Level.WARNING,"Failed to send an e-mail via SMTP",e);
            }
        }
    }
}
