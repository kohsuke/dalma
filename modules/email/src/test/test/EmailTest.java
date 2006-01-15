package test;

import dalma.ReplyIterator;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.NewMailHandler;
import dalma.endpoints.email.MimeMessageEx;
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

    protected void setupEndPoints() throws Exception {
        // passive side
        ep1 = (EmailEndPoint) engine.addEndPoint("email1",getProperty("email.endpoint1"));
        ep1.setNewMailHandler(new NewMailHandler() {
            public void onNewMail(MimeMessage mail) throws Exception {
                System.out.println("new e-mail");
                createConversation(Bob.class,ep1,mail);
            }
        });

        // active side
        ep2 = (EmailEndPoint) engine.addEndPoint("email2",getProperty("email.endpoint2"));
    }

    public void test() throws Throwable {
        for( int i=0; i<10; i++ )
            createConversation(Alice.class,ep2,ep1.getAddress());
        engine.waitForCompletion();
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

                System.out.println("A: initiating conversation. UUID="+uuid);

                // ep2 -> ep1 to iniciate a conversation
                MimeMessage msg = new MimeMessageEx(ep.getSession());
                msg.setRecipient(Message.RecipientType.TO,adrs);
                msg.setSubject("new conv");
                msg.setText(uuid.toString());
                msg = ep.waitForReply(msg);
                System.out.println("A: got a reply");

                // send multiple replies
                for( int i=0; i<10; i++ ) {
                    MimeMessage reply = (MimeMessage) msg.reply(false);
                    reply.setText(uuid.toString());
                    System.out.println("A: sending a reply");
                    ep.send(reply);
                }

                // and finally say bye
                System.out.println("A: sending bye");
                MimeMessage reply = (MimeMessage) msg.reply(false);
                reply.setText("bye");
                ep.send(reply);

                System.out.println("A: exiting");
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

                UUID uuid = UUID.fromString(msg.getContent().toString().trim());
                System.out.println("B: started "+uuid);


                msg = (MimeMessage) msg.reply(false);
                msg.setText("welcome");

                ReplyIterator<MimeMessageEx> itr = ep.waitForMultipleReplies(msg);
                while(itr.hasNext()) {
                    MimeMessage in = itr.next();
                    if(in.getContent().toString().contains("bye"))
                        break;

                    // make sure that the UUID matches
                    assertEquals(uuid,UUID.fromString(in.getContent().toString().trim()));

                    System.out.println("B: got a reply.");
                }

                System.out.println("B: done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
