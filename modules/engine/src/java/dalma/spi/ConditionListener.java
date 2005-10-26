package dalma.spi;

import dalma.Condition;

/**
 * @author Kohsuke Kawaguchi
 */
public interface ConditionListener {
    void onActivated(Condition cond);
}
