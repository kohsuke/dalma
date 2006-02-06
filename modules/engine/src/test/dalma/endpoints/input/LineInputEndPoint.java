package dalma.endpoints.input;

import dalma.Conversation;
import dalma.Condition;
import dalma.Engine;
import dalma.impl.EndPointImpl;
import dalma.spi.FiberSPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Waits for the user input.
 *
 * This is a singleton endPoint.
 *
 * @author Kohsuke Kawaguchi
 */
public final class LineInputEndPoint extends EndPointImpl implements Runnable {

    /**
     * {@link Conversation}s waiting for input.
     */
    private static final List<LineCondition> queue = new ArrayList<LineCondition>();

    private final Thread thread = new Thread(this);

    public LineInputEndPoint() {
        super(LineInputEndPoint.class.getName());
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while((line=in.readLine())!=null) {
                synchronized(queue) {
                    if(!queue.isEmpty()) {
                        // pick the conversation to be activated
                        LineCondition cond = queue.remove(0);
                        cond.activate(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new Error(e); // can never happen
        }
    }

    protected void start() {
        // start the monitor thread
        thread.start();
    }

    protected void stop() {
        thread.stop();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }
    }


    private final class LineCondition extends Condition<String> {
        public LineCondition() {
        }

        public void onParked() {
            synchronized(queue) {
                queue.add(this);
            }
        }

        public void onLoad() {
            onParked();
        }

        public void interrupt() {
            synchronized(queue) {
                queue.remove(this);
            }
        }
    }

    /**
     * Wait for an user input.
     */
    // this method is invoked from conversations
    public static String waitForInput() {
        FiberSPI<?> fiber = FiberSPI.currentFiber(true);
        return fiber.suspend(createCondition(fiber));
    }

    private static LineCondition createCondition(FiberSPI fiber) {
        LineCondition cond;
        synchronized(LineInputEndPoint.class) {
            Engine engine = fiber.getOwner().getEngine();
            LineInputEndPoint endPoint = (LineInputEndPoint)engine.getEndPoint(LineInputEndPoint.class.getName());
            cond = endPoint.new LineCondition();
        }
        return cond;
    }
}
