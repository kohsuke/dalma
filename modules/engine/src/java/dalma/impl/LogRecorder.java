package dalma.impl;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Records log data to file system, in such a way that
 * it can be retrieved later.
 *
 * <p>
 * More precisely, this class stores one log entry per one file,
 * into a directory by using the timestamp as the file name.
 *
 * <p>
 * This allows users to delete log entries manually.
 *
 * @author Kohsuke Kawaguchi
 */
public final class LogRecorder extends Handler implements Serializable {
    private final File dir;

    /**
     * View of all the current log entries as a {@link List}.
     */
    private transient /*final*/ ListView allLogs = new ListView();

    private int daysToKeepLog = -1;

    // used to be there in old version. will be removed in 1.0
    @Deprecated
    private transient int id;

    /**
     * Used to load/save logrecords.
     */
    private final ThreadLocal<XStream> xsPool = new ThreadLocal<XStream>() {
        protected XStream initialValue() {
            return new XStream();
        }
    };

    /**
     * View of recorded logs as {@link List}.
     */
    private final class ListView extends AbstractList<LogRecord> {
        private final Reloadable reloader = new Reloadable() {
            /**
             * Reload the log entries.
             * Also does the log rotation.
             */
            public void reload() {
                File[] r;
                r = dir.listFiles(new FileFilter() {
                    Date threshold;

                    {
                        if(daysToKeepLog <0) {
                            threshold = null;
                        } else {
                            Calendar cal = new GregorianCalendar();
                            cal.add(Calendar.DATE,-daysToKeepLog);
                            threshold = cal.getTime();
                        }
                    }

                    public boolean accept(File f) {
                        try {
                            String date = f.getName();
                            if(date.length()>15)
                                date = date.substring(0,15);
                            if(threshold!=null && FORMATTER.parse(date).before(threshold))
                                f.delete();
                        } catch (ParseException e) {
                            // don't recognize it as a log file,
                            // but don't delete it either
                            return false;
                        }
                        return f.getPath().endsWith(".log");
                    }
                });
                if(r!=null) {
                    files = new Vector<File>(r.length);
                    Arrays.sort(r,REVERSE_FILE_SORTER);
                    files.addAll(Arrays.asList(r));
                } else
                    files = new Vector<File>();
            }
        };

        private Vector<File> files;

        public LogRecord get(int index) {
            reloader.update();
            try {
                return ((XmlLogRecord)new XmlFile(xsPool.get(),files.get(index)).read()).get();
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to read log record",e);
                return null;
            }
        }

        public int size() {
            reloader.update();
            return files.size();
        }
    }

    public LogRecorder(File dir) {
        this.dir = dir;
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dir+" is not a directory");
    }

    /**
     * Gets a virtual {@link List} that contains all log records written thus far.
     */
    public List<LogRecord> getLogs() {
        return allLogs;
    }

    /**
     * Sets # of days to keep log entries.
     *
     * @param days
     *      -1 to keep it forever, in which case log records need to be manually
     *      deleted.
     */
    public void setDaysToKeepLog( int days ) {
        this.daysToKeepLog = days;
        allLogs.reloader.update();
    }

    public synchronized void publish(LogRecord record) {
        File data = createFile();

        try {
            new XmlFile(xsPool.get(),data).write(new XmlLogRecord(record));
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

    /**
     * Creates a new file for recoding a log entry.
     */
    private File createFile() {
        String prefix = FORMATTER.format(new Date());
        File f;
        int n=0;

        do {
            f = new File(dir, prefix+String.format("%04d",n++)+".log");
        } while(f.exists());

        if(allLogs.files!=null)
            allLogs.files.add(f);

        return f;
    }

    private void readObject(ObjectInputStream in)
               throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        allLogs = new ListView();
    }



    private static final Logger logger = Logger.getLogger(LogRecorder.class.getName());

    private static final DateFormat FORMATTER = new SimpleDateFormat("yyyyMMdd-HHmmss"); // 15 chars

    private static final Comparator<File> REVERSE_FILE_SORTER = new Comparator<File>() {
        public int compare(File lhs, File rhs) {
            return rhs.compareTo(lhs);
        }
    };
}
