package test.port.input;

import dalma.Conversation;
import dalma.spi.ConversationSPI;
import dalma.spi.port.EndPoint;
import dalma.spi.port.Dock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Waits for the user input.
 *
 * This is a singleton endPoint.
 *
 * @author Kohsuke Kawaguchi
 */
public final class LineInputEndPoint extends EndPoint implements Runnable, Serializable {

    public static final LineInputEndPoint INSTANCE = new LineInputEndPoint();

    /**
     * {@link Conversation}s waiting for input.
     */
    private static final List<LineDock> queue = new ArrayList<LineDock>();

    private LineInputEndPoint() {
        super(LineInputEndPoint.class.getName());
        // start the monitor thread
        new Thread(this).start();
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


    private static final class LineDock extends Dock<String> {
        public LineDock() {
            super(INSTANCE);
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
        final ConversationSPI cnv = ConversationSPI.getCurrentConversation();

        final LineDock dock = new LineDock();

        // FIX: this puts 'cnv' object into the stack to be restored
        return cnv.suspend(dock);
    }

    private Object readResolve() {
        return INSTANCE;
    }

    private static final long serialVersionUID = 1L;
}
