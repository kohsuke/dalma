package dalma.endpoints.email;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Picks up messages from <a href="http://en.wikipedia.org/wiki/Maildir">the maildir directory</a>.
 *
 * @author Kohsuke Kawaguchi
 */
public class MailDirListener extends Listener {
    private final File dir;
    private final int interval;
    private final Thread thread;

    private static final Logger logger = Logger.getLogger(MailDirListener.class.getName());

    public MailDirListener(File dir, int interval) {
        this.dir = dir;
        this.interval = interval;
        this.thread = new Thread(new Runner(),"MailDir listener thread for "+dir.getPath());
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
            // noop
        }
    }

    private class Runner implements Runnable {
        public void run() {
            try {
                while(true) {
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        return; // treat as a signal to die
                    }
                    try {
                        File newDir = new File(dir,"new");
                        File[] files = newDir.listFiles();
                        if(files==null)
                            continue;   // the directory doesn't exist.

                        try {
                            for(File mail : files ) {
                                BufferedInputStream in = new BufferedInputStream(new FileInputStream(mail));
                                try {
                                    MimeMessage msg;
                                    try {
                                        msg = new MimeMessage(getEndPoint().getSession(),in);
                                    } catch(OutOfMemoryError e) {
                                        // got a message that's too big
                                        logger.log(Level.SEVERE, "Failed to read "+mail+". Re-classifying to cur");
                                        in.close();
                                        mail.renameTo(new File(new File(dir,"cur"),mail.getName()));
                                        continue;
                                    }
                                    
                                    logger.fine("handling message: "+msg.getSubject());
                                    try {
                                        handleMessage(msg);
                                    } catch (MessagingException e) {
                                        logger.log(Level.WARNING,"failed to handle message: "+msg.getSubject(),e);
                                        // but delete this message anyway
                                    }
                                } finally {
                                    in.close();
                                    mail.delete();
                                }
                            }
                        } catch (IOException e) {
                            logger.log(Level.WARNING,e.getMessage(),e);
                        }
                        logger.fine("done. going back to sleep");
                    } catch (MessagingException e) {
                        logger.log(Level.WARNING,e.getMessage(),e);
                    }
                }
            } catch (Error e) {
                logger.log(Level.SEVERE,e.getMessage(),e);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }
}
