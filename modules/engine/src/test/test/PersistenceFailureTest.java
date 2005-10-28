package test;

import dalma.test.WorkflowTestProgram;
import dalma.ErrorHandler;
import dalma.TimeUnit;
import dalma.endpoints.timer.TimerEndPoint;
import junit.textui.TestRunner;

import java.io.Serializable;
import java.io.InputStream;
import java.io.NotSerializableException;

/**
 * Makes sure that the persistence failure kills the conversation cleanly.
 *
 * @author Kohsuke Kawaguchi
 */
public class PersistenceFailureTest extends WorkflowTestProgram {
    public PersistenceFailureTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(PersistenceFailureTest.class);
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
        assert reportedError instanceof NotSerializableException;
    }

    private static final class ErrorConversation implements Runnable, Serializable {
        public ErrorConversation() {
        }

        public void run() {
            System.out.println("Going to die");
            InputStream in = System.in;

            TimerEndPoint.waitFor(1000, TimeUnit.DAYS);
        }
    }
}
