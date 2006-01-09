package dalma.container;

import java.util.logging.Logger;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * @author Kohsuke Kawaguchi
 */
class LogUtil {
    /**
     * This {@link Logger} works like /dev/null, and thus suitable
     * to be used as a parent logger when logs shouldn't show up
     * in the system log.
     */
    /*package*/ static final Logger NULL_LOGGER = Logger.getAnonymousLogger();

    static {
        NULL_LOGGER.setFilter(new Filter() {
            public boolean isLoggable(LogRecord record) {
                return false;
            }
        });
    }

    static Logger newAnonymousLogger(Logger parent) {
        Logger l = Logger.getAnonymousLogger();
        l.setParent(parent);
        return l;
    }
}
