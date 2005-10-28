package test;

import dalma.test.WorkflowTestProgram;
import dalma.ErrorHandler;
import junit.textui.TestRunner;

import java.io.Serializable;

/**
 * Makes sure that an error in a conversation kills a conversation cleanly.
 * @author Kohsuke Kawaguchi
 */
public class ConversationDeathTest extends WorkflowTestProgram {
    public ConversationDeathTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(ConversationDeathTest.class);
    }

    protected void setupEndPoints() throws Exception {
        // no endpoint to set up
    }

    Throwable reportedError;

    public void test() throws Exception {
        engine.setErrorHandler(new ErrorHandler() {
            public void onError(Throwable t) {
                // make sure that the error is reported before the completion
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("error reported");
                reportedError = t;
            }
        });

        createConversation(ErrorConversation.class);
        engine.waitForCompletion();
        assert reportedError instanceof UnsupportedOperationException;
    }

    private static final class ErrorConversation implements Runnable, Serializable {
        public ErrorConversation() {
        }

        public void run() {
            System.out.println("Going to die");
            throw new UnsupportedOperationException();
        }
    }
}
