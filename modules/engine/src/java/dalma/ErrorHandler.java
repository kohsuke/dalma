package dalma;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles uncaught exceptions thrown from conversations.
 *
 * <p>
 * Conversations may throw {@link RuntimeException} because
 * of a programming error, or it may throw {@link Error}
 * because of more serious problem. Installing an {@link ErrorHandler}
 * to an {@link Engine} allows the calling application to catch
 * and report any such problem.
 *
 * @see Engine#setErrorHandler(ErrorHandler)
 *
 * @author Kohsuke Kawaguchi
 */
public interface ErrorHandler {
    /**
     * This method is invoked by the engine every time
     * a conversation throws an uncaught exception.
     *
     * @param t
     *      always non-null.
     */
    void onError(Throwable t);

    /**
     * Default error handler that sends the error to {@link Logger}.
     */
    public static final ErrorHandler DEFAULT = new ErrorHandler() {
        public void onError(Throwable t) {
            Logger.getAnonymousLogger().log(Level.SEVERE,"a conversation reported an error",t);
        }
    };
}
