package dalma.container;

import dalma.Conversation;

/**
 * Implemented by the application to specify the log preservation policy.
 *
 * @author Kohsuke Kawaguchi
 */
public interface LogRotationPolicy {
    /**
     * Called to determine if the log data of the given completed
     * conversation shall be kept or discarded.
     *
     * @param conv
     *      always non-null, valid completed conversation.
     * @return
     *      true to indicate that this log be kept. false to discard.
     */
    boolean keep(Conversation conv);

    /**
     * Default policy.
     *
     * Currently it's 7 days from completion, but may change in the future.
     */
    public static final LogRotationPolicy DEFAULT = new LogRotationPolicy() {
        public boolean keep(Conversation conv) {
            long diff = System.currentTimeMillis() - conv.getCompletionDate().getTime();
            return diff>7*24*60*60*1000;
        }
    };

    /**
     * {@link LogRotationPolicy} that keeps everything.
     */
    public static final LogRotationPolicy NEVER = new LogRotationPolicy() {
        public boolean keep(Conversation conv) {
            return true;
        }
    };
}
