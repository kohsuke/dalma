package dalma.impl;

import dalma.Condition;
import dalma.Fiber;

/**
 * {@link Condition} that waits for the completion of a {@link Fiber}.
 *
 * @author Kohsuke Kawaguchi
 */
final class FiberCompletionCondition extends Condition<Fiber> {

    /**
     * The {@link Fiber} whose completion we are blocking.
     */
    private final FiberImpl target;

    FiberCompletionCondition(FiberImpl target) {
        this.target = target;
    }

    public void onParked() {
        target.getWaitList().add(this);
    }

    public void interrupt() {
        target.getWaitList().remove(this);
    }

    public void onLoad() {
        onParked();
    }

    private static final long serialVersionUID = 1L;
}
