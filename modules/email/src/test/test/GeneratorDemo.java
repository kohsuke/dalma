package test;

import dalma.TimeUnit;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.NewMailHandler;
import dalma.endpoints.email.TCPListener;
import dalma.test.Launcher;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Iterator;
import java.util.UUID;

/**
 * Tests {@link EmailEndPoint#waitForMultipleReplies(MimeMessage)}.
 *
 * @author Kohsuke Kawaguchi
 */
public class GeneratorDemo extends Launcher implements NewMailHandler {
    public GeneratorDemo(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new GeneratorDemo(args);
    }

    EmailEndPoint ep;

    protected void setUpEndPoints() throws Exception {
        ep = new EmailEndPoint(
            "email",
            new InternetAddress("kohsuke-dalma@griffon.kohsuke.org","dalma engine"),
            new TCPListener(9920));
        ep.setNewMailHandler(this);
        engine.addEndPoint(ep);
    }

    public void onNewMail(MimeMessage mail) throws Exception {
        System.out.println("new e-mail");
        createConversation(ConversationImpl.class,ep,mail);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final EmailEndPoint ep;

        // initial e-mail
        private MimeMessage email;

        public ConversationImpl(EmailEndPoint ep, MimeMessage email) {
            this.ep = ep;
            this.email = email;
        }

        public void run() {
            try {
                UUID uuid = UUID.randomUUID();

                System.out.println("started "+uuid);
                MimeMessage msg = email;
                int count = 0;

                msg = (MimeMessage)msg.reply(false);
                msg.setText("Hello! "+uuid);

                Iterator<MimeMessage> itr = ep.waitForMultipleReplies(msg,15,TimeUnit.SECONDS);

                while(itr.hasNext()) {
                    msg = itr.next();
                    System.out.println("got a reply.");
                    if(msg.getContent().toString().contains("bye"))
                        break;

                    // reply
                    msg = (MimeMessage)msg.reply(false);
                    msg.setText("Hello! "+(count++)+'\n'+uuid.toString());
                    ep.send(msg);
                }

                MimeMessage reply = (MimeMessage)email.reply(false);
                reply.setText("bye bye");
                ep.send(reply);
                System.out.println("done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
