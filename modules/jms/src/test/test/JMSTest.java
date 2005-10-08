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

    JMSEndPoint ep;
    QueueSession qs;
    QueueConnection qcon;

    protected void setUp() throws Exception {
        super.setUp();

        qcon = new ActiveMQConnectionFactory("tcp://localhost:61616").createQueueConnection();
        qs = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        ep = new JMSEndPoint(
            "jms",
            qs, qs.createQueue("dalma-out"), qs.createQueue("dalma-in"));
        ep.setNewMessageHandler(this);
        engine.addEndPoint(ep);

        qcon.start();
    }

    public void test() throws Throwable {
        qs.createSender(qs.createQueue("dalma-in")).send(
            qs.createTextMessage("hello")
        );

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
        createConversation(ConversationImpl.class,ep,message);
    }

    public static final class ConversationImpl implements Runnable, Serializable {
        private final JMSEndPoint ep;

        // initial msg
        private Message msg;

        public ConversationImpl(JMSEndPoint ep, Message email) {
            this.ep = ep;
            this.msg = email;
        }

        public void run() {
            try {
                UUID uuid = UUID.randomUUID();

                System.out.println("started "+uuid);
                Message msg = this.msg;

                while(true) {
                    TextMessage reply = ep.createReplyMessage(TextMessage.class,msg);
                    reply.setText("Hello! "+uuid);

                    System.out.println("sent a reply");
                    msg = ep.waitForReply(msg);
                    System.out.println("got a reply");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
