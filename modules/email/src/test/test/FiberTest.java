package test;

import dalma.Fiber;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.MimeMessageEx;
import dalma.endpoints.email.NewMailHandler;
import dalma.test.WorkflowTestProgram;
import junit.framework.Assert;
import junit.textui.TestRunner;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link Fiber}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class FiberTest extends WorkflowTestProgram {

    public FiberTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(FiberTest.class);
    }
    EmailEndPoint ep1;
    EmailEndPoint ep2;

    protected void setupEndPoints() throws Exception {
        // passive side --- just send reply e-mails
        ep1 = (EmailEndPoint) engine.addEndPoint("email1",getProperty("email.endpoint1"));
        ep1.setNewMailHandler(new NewMailHandler() {
            int counter;
            public void onNewMail(MimeMessage mail) throws Exception {
                MimeMessage m = (MimeMessage) mail.reply(false);
                m.setText(String.valueOf(counter++));
                ep1.send(m);
            }
        });

        // active side
        ep2 = (EmailEndPoint) engine.addEndPoint("email2",getProperty("email.endpoint2"));
    }


    public void test() throws Throwable {
        createConversation(Alice.class,ep2,ep1.getAddress());
        engine.waitForCompletion();
    }

    /**
     * Use multiple fibers.
     */
    static final class Alice implements Runnable, Serializable {
        private final EmailEndPoint ep;
        private final InternetAddress adrs;

        public Alice(EmailEndPoint ep,InternetAddress adrs) {
            this.ep = ep;
            this.adrs = adrs;
        }

        final class ReqRsp implements Runnable, Serializable {
            int value;

            final String id;

            public ReqRsp(String id) {
                this.id = id;
            }

            public void run() {
                try {
                    MimeMessage msg = new MimeMessageEx(ep.getSession());
                    msg.setRecipient(Message.RecipientType.TO,adrs);
                    msg.setSubject("new conv");
                    msg.setText("abc");
                    log("sending a message");
                    msg = ep.waitForReply(msg);
                    log("got a reply");
                    value = Integer.parseInt(msg.getContent().toString().trim());
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail();
                }
            }

            private void log(String msg) {
                System.out.println(id+": "+msg);
            }
        }

        public void run() {
            try {
                List<Fiber<ReqRsp>> fibers = new ArrayList<Fiber<ReqRsp>>();

                System.out.println("A: initiating fibers");
                for (int i = 0; i < 10; i++) {
                    Fiber<ReqRsp> f = Fiber.create(new ReqRsp("Alice" + i));
                    fibers.add(f);
                    f.start();
                }

                System.out.println("A: waiting for fibers to complete");

                for (Fiber<ReqRsp> fiber : fibers.toArray(new Fiber[0])) {
                    fiber.join();
                }

                int value = 0;

                for (Fiber<ReqRsp> fiber : fibers) {
                    value += fiber.getRunnable().value;
                }

                Assert.assertEquals(45 /* 1/2*N*(N-1) with N=10 */, value);

                System.out.println("A: exiting");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
    }
}
