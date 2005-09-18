package test;

import test.infra.Launcher;
import test.port.timer.TimerEndPoint;

import java.io.Serializable;

import static dalma.TimeUnit.SECONDS;
import dalma.ports.email.EmailEndPointImpl;
import dalma.ports.email.EmailEndPoint;
import dalma.spi.port.EndPoint;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

/**
 * @author Kohsuke Kawaguchi
 */
public class EmailTest extends Launcher {
    public EmailTest(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new EmailTest(args);
    }

    protected void init() throws Exception {
        EmailEndPoint ep = new EmailEndPointImpl("email",new InternetAddress("dalma@kohsuke.org","dalma engine"));
        engine.addEndPoint(ep);

        createConversation(ConversationImpl.class,ep);
        createConversation(ConversationImpl.class,ep);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final EmailEndPoint ep;

        public ConversationImpl(EmailEndPoint ep) {
            this.ep = ep;
        }

        public void run() {
            try {
                Message msg = new MimeMessage(Session.getInstance(System.getProperties()));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress("kk@kohsuke.org"));
                msg.setText("Hello!");
                msg = ep.waitForReply(msg);

                Message reply = msg.reply(false);
                msg.setText("Reply to "+msg.getSubject());

                Transport.send(reply);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
