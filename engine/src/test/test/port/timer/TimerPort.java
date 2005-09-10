package test.port.timer;

import dalma.spi.ConversationSPI;
import dalma.spi.port.Dock;
import dalma.spi.port.Port;

import java.io.Serializable;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Port that waits for some time to pass.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerPort implements Port, Serializable {

    public static final TimerPort INSTANCE = new TimerPort();

    private static final Timer timer = new Timer(true);

    private TimerPort() {
    }

    private static final class TimerDock extends Dock<Void> {
        /**
         * The date when the conversation should be activated.
         */
        private final Date dt;

        /**
         *
         * Transient because this field is only used when the timer is in memory.
         */
        private transient TimerTaskImpl task;

        public TimerDock(Date dt) {
            super(INSTANCE);
            this.dt = dt;
        }

        public void park() {
            assert task==null;
            task = new TimerTaskImpl();
            timer.schedule(task,dt);
        }

        public void onLoad() {
            park();
        }

        public void interrupt() {
            assert task!=null;
            task.cancel();
            task = null;
        }

        private final class TimerTaskImpl extends TimerTask {
            public void run() {
                TimerDock.this.resume(null);
            }
        }
    }

    /**
     * Wait for an user input.
     */
    // this method is invoked from conversations
    public static void waitFor(long delay) {
        waitFor(new Date(System.currentTimeMillis()+delay));
    }

    public static void waitFor(Date dt) {
        ConversationSPI.getCurrentConversation().suspend(new TimerDock(dt));
    }

    private Object readResolve() {
        return INSTANCE;
    }

    private static final long serialVersionUID = 1L;
}
