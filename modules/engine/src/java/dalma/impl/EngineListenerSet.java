package dalma.impl;

import dalma.EngineListener;
import dalma.Conversation;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Set of {@link EngineListener}.
 *
 * Invoking the {@link EngineListener} methods on this object will fire the same event
 * to all listeners.
 *
 * @author Kohsuke Kawaguchi
 */
final class EngineListenerSet extends EngineListener {
    private final Set<EngineListener> listeners = Collections.synchronizedSet(new HashSet<EngineListener>());

    private static final Logger logger = Logger.getLogger(EngineListenerSet.class.getName());

    public void add(EngineListener l) {
        listeners.add(l);
    }

    public void remove(EngineListener l) {
        if(!listeners.remove(l))
            throw new IllegalArgumentException();
    }

    private EngineListener[] getListeners() {
        return listeners.toArray(new EngineListener[listeners.size()]);
    }

    private void error(Throwable e) {
        logger.log(Level.WARNING, "EngineListener reported an error",e);
    }

    public void onConversationStarted(Conversation conv) {
        for (EngineListener l : getListeners() ) {
            try {
                l.onConversationStarted(conv);
            } catch (Throwable e) {
                error(e);
            }
        }
    }

    public synchronized void onConversationCompleted(Conversation conv) {
        for (EngineListener l : getListeners() ) {
            try {
                l.onConversationCompleted(conv);
            } catch (Exception e) {
                error(e);
            }
        }
    }
}
