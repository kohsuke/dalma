package dalma.endpoints.timer;

import dalma.Condition;
import dalma.TimeUnit;
import dalma.impl.EndPointImpl;
import dalma.spi.ConversationSPI;
import dalma.spi.EngineSPI;
import dalma.spi.FiberSPI;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * EndPoint that waits for some time to pass.
 *
 * <p>
 * The {@link #createDock(Date)} method can return a {@link Condition} of an
 * arbitrary type, because it always return null. This works better when
 * timer is used with other {@link Condition}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class TimerEndPoint extends EndPointImpl {

    private static final Timer timer = new Timer(true);

    public TimerEndPoint() {
        super(TimerEndPoint.class.getName());
    }

    private final class TimerCondition<T> extends Condition<T> {
        /**
         * The date when the conversation should be activated.
         */
        private final Date dt;

        /**
         *
         * Transient because this field is only used when the timer is in memory.
         */
        private transient TimerTaskImpl task;

        public TimerCondition(Date dt) {
            assert dt!=null;
            this.dt = dt;
        }

        public void onParked() {
            assert task==null;
            task = new TimerTaskImpl();
            timer.schedule(task,dt);
        }

        public void onLoad() {
            onParked();
        }

        public void interrupt() {
            assert task!=null;
            task.cancel();
            task = null;
        }

        private final class TimerTaskImpl extends TimerTask {
            public void run() {
                TimerCondition.this.activate(null);
            }
        }
    }

    protected void start() {
        // nothing to do
    }

    protected void stop() {
        timer.cancel();
    }

    /**
     * Wait for an user input.
     */
    // this method is invoked from conversations
    public static void waitFor(long delay,TimeUnit unit) {
        FiberSPI.currentFiber(true).suspend(createDock(delay,unit));
    }

    public static void waitFor(Date dt) {
        FiberSPI.currentFiber(true).suspend(createDock(dt));
    }

    public static <T> Condition<T> createDock(Date dt) {
        EngineSPI engine = ConversationSPI.currentConversation().getEngine();

        TimerEndPoint ep = (TimerEndPoint)engine.getEndPoint(TimerEndPoint.class.getName());
        if(ep==null) {
            throw new IllegalStateException("TimerEndPoint was not added to the engine");
        }
        return ep.new TimerCondition<T>(dt);
    }

    public static <T> Condition<T> createDock(long delay,TimeUnit unit) {
        return createDock(unit.fromNow(delay));
    }
}
