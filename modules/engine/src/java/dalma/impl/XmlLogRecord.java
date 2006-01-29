package dalma.impl;

import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * Persistence form of {@link LogRecord}.
 *
 * @author Kohsuke Kawaguchi
 */
public class XmlLogRecord {
    public String level;
    public String loggerName;
    public String message;
    public Date time;
    public String[] parameters;
    public long sequenceNumber;
    public String sourceClassName;
    public String sourceMethodName;
    public int threadId;
    public RecordableException thrown;

    public XmlLogRecord() {}
    public XmlLogRecord(LogRecord lr) {
        set(lr);
    }

    public void set(LogRecord lr) {
        this.level = lr.getLevel().getName();
        this.loggerName = lr.getLoggerName();
        this.message = lr.getMessage();
        this.time = new Date(lr.getMillis());
        Object[] params = lr.getParameters();
        if(params!=null) {
            String[] r = new String[params.length];
            for( int i=0; i<r.length; i++ ) {
                if(params[i]!=null)
                    r[i] = params[i].toString();
                else
                    r[i] = null;
            }
            this.parameters = r;
        }
        this.sequenceNumber = lr.getSequenceNumber();
        this.sourceClassName = lr.getSourceClassName();
        this.sourceMethodName = lr.getSourceMethodName();
        this.threadId = lr.getThreadID();
        this.thrown = RecordableException.create(lr.getThrown());
    }

    public LogRecord get() {
        LogRecord r = new LogRecord(Level.parse(level),message);
        r.setLoggerName(loggerName);
        r.setParameters(parameters);
        r.setSequenceNumber(sequenceNumber);
        r.setSourceClassName(sourceClassName);
        r.setSourceMethodName(sourceMethodName);
        r.setThreadID(threadId);
        r.setThrown(thrown);
        if(time!=null)
            r.setMillis(time.getTime());
        return r;
    }
}
