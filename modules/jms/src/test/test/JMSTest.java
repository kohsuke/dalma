package test;

import dalma.endpoints.jms.JMSEndPoint;
import dalma.endpoints.jms.MessageHandler;
import dalma.test.WorkflowTestProgram;
import junit.textui.TestRunner;
import org.activemq.ActiveMQConnectionFactory;

import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.JMSException;
import java.io.Serializable;
import java.util.UUID;

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

    protected void setUp() throws Exception {
        super.setUp();

        qcon = new ActiveMQConnectionFactory("tcp://localhost:61616").createQueueConnection();
        qs = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue out = qs.createQueue("dalma-out");
        Queue in = qs.createQueue("dalma-in");

        ep1 = new JMSEndPoint("jms1", qs, out, in);
        ep1.setNewMessageHandler(this);
        engine.addEndPoint(ep1);

        ep2 = new JMSEndPoint("jms2", qs, in, out);
        engine.addEndPoint(ep2);

        qcon.start();
    }

    public void test() throws Throwable {
        createConversation(Alice.class,ep1);

        // for now
        Thread.sleep(3000);
        engine.waitForCompletion();
        engine.checkError();
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
                msg.setText("Hello");
                msg = (TextMessage)ep.waitForReply(msg);
                System.out.println("A: Bye");
                msg = ep.createReplyMessage(TextMessage.class,msg);
                msg.setText("bye");
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
        private Message msg;

        public Bob(JMSEndPoint ep, Message email) {
            this.ep = ep;
            this.msg = email;
        }

        public void run() {
            try {
                UUID uuid = UUID.randomUUID();

                System.out.println("B: started "+uuid);
                Message msg = this.msg;

                TextMessage reply = ep.createReplyMessage(TextMessage.class,msg);
                reply.setText("Hello! "+uuid);

                System.out.println("B: Hello back");
                msg = ep.waitForReply(reply);
                System.out.println("B: dying");
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
