package test;

import dalma.ReplyIterator;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.NewMailHandler;
import dalma.test.WorkflowTestProgram;
import junit.textui.TestRunner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Kohsuke Kawaguchi
 */
public class EmailTest extends WorkflowTestProgram {

    public EmailTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        TestRunner.run(EmailTest.class);
    }

    EmailEndPoint ep1;
    EmailEndPoint ep2;

    protected void setUp() throws Exception {
        super.setUp();

        // passive side
        ep1 = (EmailEndPoint) engine.addEndPoint("email",getProperty("email.endpoint1"));
        ep1.setNewMailHandler(new NewMailHandler() {
            public void onNewMail(MimeMessage mail) throws Exception {
                System.out.println("new e-mail");
                createConversation(Bob.class,ep1,mail);
            }
        });

        // active side
        ep2 = (EmailEndPoint) engine.addEndPoint("email",getProperty("email.endpoint2"));
    }

    @Override
    protected void runTest() throws Throwable {
        createConversation(Alice.class,ep2);
    }

    public static final class Alice implements Runnable, Serializable {
        private final EmailEndPoint ep;
        private final InternetAddress adrs;

        public Alice(EmailEndPoint ep,InternetAddress adrs) {
            this.ep = ep;
            this.adrs = adrs;
        }

        public void run() {
            try {
                UUID uuid = UUID.randomUUID();

                // ep2 -> ep1 to iniciate a conversation
                MimeMessage msg = new MimeMessage(ep.getSession());
                msg.setRecipient(Message.RecipientType.TO,adrs);
                msg.setSubject("new conv");
                msg.setText(uuid.toString());
                msg = ep.waitForReply(msg);

                // send multiple replies
                for( int i=0; i<10; i++ ) {
                    MimeMessage reply = (MimeMessage) msg.reply(false);
                    reply.setText(uuid.toString());
                    ep.send(reply);
                }

                // and finally say bye
                MimeMessage reply = (MimeMessage) msg.reply(false);
                reply.setText("bye");
                ep.send(reply);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class Bob implements Runnable, Serializable {
        private final EmailEndPoint ep;

        // initial e-mail
        private MimeMessage email;

        public Bob(EmailEndPoint ep, MimeMessage email) {
            this.ep = ep;
            this.email = email;
        }

        public void run() {
            try {
                MimeMessage msg = email;

                UUID uuid = UUID.fromString(msg.getContent().toString());
                System.out.println("started "+uuid);


                msg = (MimeMessage) msg.reply(false);
                msg.setText("welcome");

                ReplyIterator<MimeMessage> itr = ep.waitForMultipleReplies(msg);
                while(itr.hasNext()) {
                    MimeMessage in = itr.next();
                    if(in.getContent().toString().contains("bye"))
                        break;

                    // make sure that the UUID matches
                    assertEquals(uuid,UUID.fromString(msg.getContent().toString()));

                    System.out.println("got a reply.");
                }

                System.out.println("done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
