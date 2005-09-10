package test;

import test.infra.Launcher;
import test.port.timer.TimerPort;
import dalma.Conversation;

import java.io.Serializable;

/**
 * Tests waiting for other conversations.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConversationPortTest extends Launcher {
    public ConversationPortTest(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new ConversationPortTest(args).run();
    }

    private Conversation conv;

    protected void init() throws Exception {
        Conversation conv1 = createConversation(ClickConversation.class);
        conv = createConversation(WaitConversation.class,conv1);
    }

    private void run() throws Exception {
        conv.join();
        System.out.println("completed");
        System.exit(0);
    }

    public static final class WaitConversation implements Runnable, Serializable {
        private final Conversation conv;

        public WaitConversation(Conversation conv) {
            this.conv = conv;
        }

        public void run() {
            try {
                conv.join();
            } catch (InterruptedException e) {
            }
            System.out.println("joined");
        }
        private static final long serialVersionUID = 1L;
    }
}
