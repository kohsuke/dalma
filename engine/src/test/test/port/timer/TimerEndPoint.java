package test.port.timer;

import dalma.spi.ConversationSPI;
import dalma.spi.port.Dock;
import dalma.spi.port.EndPoint;
import dalma.TimeUnit;

import java.io.Serializable;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * EndPoint that waits for some time to pass.
 *
 * <p>
 * The {@link #createDock(Date)} method can return a {@link Dock} of an
 * arbitrary type, because it always return null. This works better when
 * timer is used with other {@link Dock}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerEndPoint extends EndPoint implements Serializable {

    public static final TimerEndPoint INSTANCE = new TimerEndPoint();

    private static final Timer timer = new Timer(true);

    private TimerEndPoint() {
        super(TimerEndPoint.class.getName());
    }

    private static final class TimerDock<T> extends Dock<T> {
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
    public static void waitFor(long delay,TimeUnit unit) {
        ConversationSPI.getCurrentConversation().suspend(createDock(delay,unit));
    }

    public static void waitFor(Date dt) {
        ConversationSPI.getCurrentConversation().suspend(createDock(dt));
    }

    public static <T> Dock<T> createDock(Date dt) {
        return new TimerDock<T>(dt);
    }

    public static <T> Dock<T> createDock(long delay,TimeUnit unit) {
        return new TimerDock<T>(new Date(System.currentTimeMillis()+unit.toMilli(delay)));
    }

    private Object readResolve() {
        return INSTANCE;
    }

    private static final long serialVersionUID = 1L;
}
