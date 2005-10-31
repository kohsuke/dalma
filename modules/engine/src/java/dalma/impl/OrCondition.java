package dalma.impl;

import dalma.Condition;
import dalma.spi.ConditionListener;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class OrCondition<T> extends Condition<Condition<? extends T>> implements ConditionListener {

    /**
     * {@link Condition}s that this object is waiting on.
     * If any one of them become active, this object becomes active.
     */
    private final List<Condition<? extends T>> conditions;

    public OrCondition(Condition<? extends T>... conditions) {
        this(Arrays.asList(conditions));
    }

    public OrCondition(List<Condition<? extends T>> conditions) {
        if(conditions.isEmpty())
            throw new IllegalArgumentException("condition list is empty");
        this.conditions = conditions;
    }

    public void onParked() {
        for (Condition<? extends T> co : conditions) {
            co.park(this);
        }
    }

    public void interrupt() {
        for (Condition<? extends T> co : conditions) {
            co.interrupt();
        }
    }

    public void onLoad() {
        for (Condition<? extends T> co : conditions) {
            co.onLoad();
        }
    }

    public synchronized void onActivated(Condition cond) {
        if(isActive()) {
            // we are already active. ignore
            return;
        }
        activate(cond);
        for (Condition<? extends T> c : conditions) {
            if(c!=cond)
                c.interrupt();
        }
    }

    private static final long serialVersionUID = 1L;
}
