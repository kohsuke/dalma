package dalma.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.List;
import java.util.AbstractList;

/**
 * Records log data to file system, in such a way that
 * it can be retrieved later.
 *
 * <p>
 * More precisely, this class stores one log entry per one file,
 * into a directory.
 *
 * @author Kohsuke Kawaguchi
 */
final class LogRecorder extends Handler implements Serializable {
    private final File dir;
    private int id = 0;

    private static final Logger logger = Logger.getLogger(LogRecorder.class.getName());

    /**
     * View of all the current log entries as a {@link List}.
     */
    private transient /*final*/ ListView allLogs = new ListView();

    private final class ListView extends AbstractList<LogRecord> {
        public LogRecord get(int index) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(getFile(index))));
                try {
                    return (LogRecord)ois.readObject();
                } finally {
                    ois.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to read log record",e);
                return null;
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING,"Failed to read log record",e);
                return null;
            }
        }

        public int size() {
            return id;
        }
    }

    public LogRecorder(File dir) {
        this.dir = dir;
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dir+" is not a directory");
        // TODO: implement faster binary search like search
        while(getFile(id).exists())
            id++;
    }

    /**
     * Gets a virtual {@link List} that contains all log records written thus far.
     */
    public List<LogRecord> getLogs() {
        return allLogs;
    }

    public synchronized void publish(LogRecord record) {
        File data;

        do {
            data = getFile(id);
            id++;
        } while(data.exists());

        try {
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(data)));
            try {
                os.writeObject(record);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING,"Failed to write log record",e);
            // just throw away this log record
        }
    }

    public void flush() {
        // noop
    }

    public void close() throws SecurityException {
        // noop
    }

    private File getFile(int n) {
        return new File(dir,String.format("%06d",n));
    }

    private void readObject(ObjectInputStream in)
   			throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        allLogs = new ListView();
    }
}
