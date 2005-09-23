package test;

import dalma.ports.email.EmailEndPoint;
import dalma.ports.email.MimeMessageEx;
import dalma.ports.email.POP3Listener;
import test.infra.Launcher;
import test.infra.PasswordStore;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.UUID;

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

    EmailEndPoint ep;

    protected void setUpEndPoints() throws Exception {
        ep = new EmailEndPoint(
            "email",
            new InternetAddress("dalma@kohsuke.org","dalma engine"),
            new POP3Listener("mail.kohsuke.org","dalma",PasswordStore.get("dalma@kohsuke.org"),3000));
        engine.addEndPoint(ep);
    }

    protected void init() throws Exception {
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
                UUID uuid = UUID.randomUUID();

                MimeMessage msg = new MimeMessage(Session.getInstance(System.getProperties()));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress("kk@kohsuke.org"));
                msg.setText("Hello! "+uuid.toString());
                msg.setSubject("testing dalma");
                msg = new MimeMessageEx(msg);
                msg = ep.waitForReply(msg);

                System.out.println("got a reply.");

                MimeMessage reply = (MimeMessage)msg.reply(false);
                reply.setText("Reply to "+msg.getSubject()+" "+uuid.toString());
                ep.send(reply);
                System.out.println("done");
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
