package dalma.spi;

import dalma.Condition;
import dalma.Conversation;
import dalma.Fiber;
import dalma.impl.OrCondition;
import dalma.impl.ConversationImpl;
import dalma.impl.FiberImpl;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class FiberSPI implements Fiber {
    /**
     * Suspends the conversation.
     *
     * @param condition
     *      The condition to which this conversation is parked.
     *      Must not be null. This condition is resonpsible for
     *      resuming this conversation.
     * @return
     *      This method returns when the conversation is resumed.
     *      the parameter given to {@link Condition#activate(Object)} will be
     *      returned.
     */
    public abstract <T> T suspend(Condition<T> condition);

    public final <T> T suspend(List<? extends Condition<? extends T>> conditions) {
        return suspend(new OrCondition<T>(new ArrayList<Condition<? extends T>>(conditions))).getReturnValue();
    }

    public final <T> T suspend(Condition<? extends T>... conditions) {
        if(conditions ==null)
            throw new IllegalArgumentException("condition list is null");
        return suspend(new OrCondition<T>(conditions)).getReturnValue();
    }

    public abstract ConversationSPI getOwner();

    /**
     * Returns the {@link FiberSPI} that the current thread is executing.
     */
    public static FiberSPI currentFiber() {
        return FiberImpl.currentFiber();
    }
}
