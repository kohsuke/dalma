package test;

import test.infra.Launcher;
import test.port.timer.TimerPort;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class TimerTest {
    public static void main(String[] args) throws Exception {
        Launcher.main(TimerConversation.class);
    }

    public static final class TimerConversation implements Runnable, Serializable {
        public void run() {
            for( int i=0; i<100; i++ ) {
                System.out.println("waiting "+i);
                TimerPort.waitFor(5*1000);  // 5 sec
            }
        }
        private static final long serialVersionUID = 1L;
    }
}
