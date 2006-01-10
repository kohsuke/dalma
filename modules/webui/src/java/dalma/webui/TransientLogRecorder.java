package dalma.webui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * {@link Handler} that simply buffers {@link LogRecord}.
 *
 * @author Kohsuke Kawaguchi
 */
public class TransientLogRecorder extends Handler {
    private final List<LogRecord> buf = new LinkedList<LogRecord>();
    private static final int MAX = 100;

    public synchronized List<LogRecord> getLogRecords() {
        // need to copy to avoid synchronization issue
        return new ArrayList<LogRecord>(buf);
    }

    public synchronized void publish(LogRecord record) {
        buf.add(0,record);
        if(buf.size()>MAX)
            buf.remove(buf.size()-1);
    }

    public void flush() {
        // noop
    }

    public void close() throws SecurityException {
        // noop
    }
}
