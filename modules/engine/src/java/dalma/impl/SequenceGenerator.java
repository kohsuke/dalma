package dalma.impl;

import java.io.Serializable;

/**
 * Sequence number generator.
 *
 * @author Kohsuke Kawaguchi
 */
final class SequenceGenerator implements Serializable {
    private int iota = 0;

    public synchronized int next() {
        return iota++;
    }

    private static final long serialVersionUID = 1L;
}
