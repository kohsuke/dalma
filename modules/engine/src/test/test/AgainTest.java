package test;

import dalma.Fiber;
import static dalma.TimeUnit.SECONDS;
import dalma.endpoints.timer.TimerEndPoint;
import dalma.endpoints.input.LineInputEndPoint;
import dalma.test.WorkflowTestProgram;
import junit.textui.TestRunner;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class AgainTest extends WorkflowTestProgram {
    public AgainTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(AgainTest.class);
    }

    protected void setupEndPoints() throws Exception {
        // no endpoint to set up
        engine.addEndPoint(new LineInputEndPoint());
    }

    public void test() throws Exception {
        createConversation(AgainConversation.class);
        engine.waitForCompletion();
        assert retryCount==3;
    }

    private static int retryCount = 0;

    public static final class AgainConversation implements Runnable, Serializable {
        public void run() {
            retryCount = 0;
            int[] t = new int[1];

            TimerEndPoint.waitFor(1, SECONDS);
            //String s = LineInputEndPoint.waitForInput();
            //System.out.println("Received: "+s);
            retryCount++;

            if(t[0]<3) {
                System.out.println("again");
                t[0]++;
                Fiber.again(3, SECONDS);
            }
            System.out.println("done");
        }
        private static final long serialVersionUID = 1L;
    }
}
