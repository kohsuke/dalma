package dalma.endpoints.timer;

import dalma.Dock;
import dalma.TimeUnit;
import dalma.impl.EndPointImpl;
import dalma.spi.ConversationSPI;
import dalma.spi.EngineSPI;

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
public class TimerEndPoint extends EndPointImpl {

    private static final Timer timer = new Timer(true);

    public TimerEndPoint() {
        super(TimerEndPoint.class.getName());
    }

    private final class TimerDock<T> extends Dock<T> {
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
            super(TimerEndPoint.this);
            assert dt!=null;
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

    protected void stop() {
        timer.cancel();
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
        EngineSPI engine = ConversationSPI.getCurrentConversation().getEngine();

        synchronized(TimerEndPoint.class) {
            TimerEndPoint ep = (TimerEndPoint)engine.getEndPoint(TimerEndPoint.class.getName());
            if(ep==null) {
                // make sure no two threads try to create a new endpoint at the same time.
                ep = new TimerEndPoint();
                engine.addEndPoint(ep);
            }
            return ep.new TimerDock<T>(dt);
        }
    }

    public static <T> Dock<T> createDock(long delay,TimeUnit unit) {
        return createDock(unit.fromNow(delay));
    }
}
