package test.port.input;

import dalma.Conversation;
import dalma.Dock;
import dalma.Engine;
import dalma.impl.EndPointImpl;
import dalma.spi.ConversationSPI;

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
    private static final List<LineDock> queue = new ArrayList<LineDock>();

    private final Thread thread = new Thread(this);

    private LineInputEndPoint() {
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
                        LineDock dock = queue.remove(0);
                        dock.resume(line);
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
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }
    }


    private final class LineDock extends Dock<String> {
        public LineDock() {
            super(LineInputEndPoint.this);
        }

        public void park() {
            synchronized(queue) {
                queue.add(this);
            }
        }

        public void onLoad() {
            park();
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
        ConversationSPI cnv = ConversationSPI.getCurrentConversation();
        return cnv.suspend(createDock(cnv));
    }

    private static LineDock createDock(ConversationSPI cnv) {
        LineDock dock;
        synchronized(LineInputEndPoint.class) {
            Engine engine = cnv.getEngine();
            LineInputEndPoint endPoint = (LineInputEndPoint)engine.getEndPoint(LineInputEndPoint.class.getName());
            if(endPoint==null) {
                endPoint = new LineInputEndPoint();
                engine.addEndPoint(endPoint);
            }
            dock = endPoint.new LineDock();
        }
        return dock;
    }
}
