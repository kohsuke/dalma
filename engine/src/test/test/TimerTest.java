package test;

import static dalma.TimeUnit.SECONDS;
import test.infra.Launcher;
import test.port.timer.TimerPort;

import java.io.Serializable;

/**
 * Tests timer port.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerTest extends Launcher {
    public TimerTest(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new TimerTest(args);
    }

    protected void init() throws Exception {
        createConversation(TimerConversation.class);
    }

    public static final class TimerConversation implements Runnable, Serializable {
        public void run() {
            for( int i=0; i<100; i++ ) {
                System.out.println("waiting "+i);
                TimerPort.waitFor(5,SECONDS);  // 5 sec
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
