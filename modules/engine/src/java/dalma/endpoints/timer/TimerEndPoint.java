package dalma.endpoints.timer;

import dalma.Condition;
import dalma.TimeUnit;
import dalma.Fiber;
import dalma.impl.EndPointImpl;
import dalma.impl.FiberImpl;
import dalma.spi.ConversationSPI;
import dalma.spi.EngineSPI;
import dalma.spi.FiberSPI;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

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

    private Timer timer;

    /**
     * Timers that were queued while the endpoint is stopped.
     */
    private List<TimerCondition> queuedConditions = new ArrayList<TimerCondition>();

    public TimerEndPoint() {
        super(TimerEndPoint.class.getName());
    }

    private final class TimerCondition<T> extends Condition<T> {
        /**
         * The date when the conversation should be activated.
         */
        private final Date dt;

        /**
         * This field is used to implement
         * {@link Fiber#doItAgain(Date)}.
         */
        private final Object presetRetVal;

        /**
         *
         * Transient because this field is only used when the timer is in memory.
         */
        private transient TimerTaskImpl task;

        public TimerCondition(Date dt, Object presetRetVal) {
            assert dt!=null;
            this.dt = dt;
            this.presetRetVal = presetRetVal;
        }

        public void onParked() {
            assert task==null;
            task = new TimerTaskImpl();
            synchronized(TimerEndPoint.this) {
                if(timer==null)
                    queuedConditions.add(this);
                else
                    timer.schedule(task,dt);
            }
        }

        public void onLoad() {
            onParked();
        }

        public void interrupt() {
            assert task!=null;
            task.cancel();
            task = null;
        }

        public void activate(T retVal) {
            super.activate((T)presetRetVal);
        }

        private final class TimerTaskImpl extends TimerTask {
            public void run() {
                TimerCondition.this.activate(null);
            }
        }
    }

    protected synchronized void start() {
        timer = new Timer(true);
        for (TimerCondition tc : queuedConditions)
            timer.schedule(tc.task,tc.dt);
        queuedConditions.clear();
    }

    protected synchronized void stop() {
        timer.cancel();
        timer = null;
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

    /**
     * This version is used internally by Dalma.
     * Not for client applications.
     */
    public static <T> Condition<T> xxxCreateDock(Date dt, Object retVal) {
        EngineSPI engine = ConversationSPI.currentConversation().getEngine();

        TimerEndPoint ep = (TimerEndPoint)engine.getEndPoint(TimerEndPoint.class.getName());
        if(ep==null) {
            throw new IllegalStateException("TimerEndPoint was not added to the engine");
        }
        return ep.new TimerCondition<T>(dt,retVal);
    }

    public static <T> Condition<T> createDock(long delay,TimeUnit unit) {
        return createDock(unit.fromNow(delay));
    }

    public static <T> Condition<T> createDock(Date dt) {
        return xxxCreateDock(dt,null);
    }
}
