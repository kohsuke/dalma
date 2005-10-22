package test;

import dalma.endpoints.jms.JMSEndPoint;
import dalma.endpoints.jms.MessageHandler;
import dalma.test.WorkflowTestProgram;
import junit.textui.TestRunner;
import org.activemq.ActiveMQConnectionFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class JMSTest extends WorkflowTestProgram implements MessageHandler {
    public JMSTest(String name) throws Exception {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        TestRunner.run(JMSTest.class);
    }

    JMSEndPoint ep1;
    JMSEndPoint ep2;
    QueueSession qs;
    QueueConnection qcon;

    protected void setupEndPoints() throws Exception {
        qcon = new ActiveMQConnectionFactory("tcp://localhost:61616").createQueueConnection();
        qs = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue out = qs.createQueue("dalma-out");
        Queue in = qs.createQueue("dalma-in");

        ep1 = new JMSEndPoint("jms1", qs, out, in);
        engine.addEndPoint(ep1);

        ep2 = new JMSEndPoint("jms2", qs, in, out);
        ep2.setNewMessageHandler(this);
        engine.addEndPoint(ep2);

        qcon.start();
    }

    public void test() throws Throwable {
        createConversation(Alice.class,ep1);

        // for now
        Thread.sleep(3000);
        engine.waitForCompletion();
    }

    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            qs.close();
            qcon.close();
        }
    }

    public void onNewMessage(Message message) throws Exception {
        System.out.println("new message");
        createConversation(Bob.class,ep2,message);
    }

    /**
     * Activating side.
     */
    public static final class Alice implements Runnable, Serializable {
        private final JMSEndPoint ep;

        public Alice(JMSEndPoint ep) {
            this.ep = ep;
        }

        public void run() {
            try {
                System.out.println("A: Hello");
                TextMessage msg = ep.createMessage(TextMessage.class);
                msg.setText("A:Hello");
                msg = (TextMessage)ep.waitForReply(msg);

                System.out.println("A: Got "+msg.getText());
                assertTrue(msg.getText().contains("B:"));
                System.out.println("A: Bye");
                msg = ep.createReplyMessage(TextMessage.class,msg);
                msg.setText("A: Bye");
                ep.send(msg);
            } catch (JMSException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Passive side.
     */
    public static final class Bob implements Runnable, Serializable {
        private final JMSEndPoint ep;

        // initial msg
        private TextMessage msg;

        public Bob(JMSEndPoint ep, TextMessage email) {
            this.ep = ep;
            this.msg = email;
        }

        public void run() {
            try {
                TextMessage msg = this.msg;

                System.out.println("B: Got "+msg.getText());
                assertTrue(msg.getText().contains("A:"));

                TextMessage reply = ep.createReplyMessage(TextMessage.class,msg);
                reply.setText("B: Hello back");
                System.out.println("B: Hello back");
                msg = (TextMessage)ep.waitForReply(reply);

                System.out.println("B: Got "+msg.getText());
                assertTrue(msg.getText().contains("A:"));
                System.out.println("B: dying");
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
