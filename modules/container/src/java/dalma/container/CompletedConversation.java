package dalma.container;

import dalma.Conversation;
import dalma.Engine;
import dalma.ConversationState;
import dalma.impl.XmlFile;

import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;

/**
 * {@link Conversation} that represents a completed one.
 *
 * <p>
 * This object is immutable. From outside this object is accessed just as
 * {@link Conversation}.
 *
 * @author Kohsuke Kawaguchi
 */
final class CompletedConversation implements Conversation, Serializable {
    private final int id;
    private final String title;
    private final long startDate;
    private final long endDate;
    private final LogRecord[] logs;

    private transient List<LogRecord> logView;

    /**
     * Creates a new {@link CompletedConversation} from another {@link Conversation}.
     */
    CompletedConversation(Conversation that) {
        this.id = that.getId();
        this.title = that.getTitle();
        this.startDate = that.getStartDate().getTime();
        this.endDate = that.getCompletionDate().getTime();

        List<LogRecord> ll = that.getLog();
        this.logs = ll.toArray(new LogRecord[ll.size()]);
    }

    public int getId() {
        return id;
    }

    public Engine getEngine() {
        throw uoe();
    }

    public ConversationState getState() {
        return ConversationState.ENDED;
    }

    public void remove() {
        throw uoe();
    }

    public void join() {
        throw uoe();
    }

    public String getTitle() {
        return title;
    }

    public List<LogRecord> getLog() {
        if(logView==null)
            logView = Collections.unmodifiableList(Arrays.asList(logs));
        return logView;
    }

    public Date getStartDate() {
        return new Date(startDate);
    }

    public Date getCompletionDate() {
        return new Date(endDate);
    }

    private UnsupportedOperationException uoe() {
        return new UnsupportedOperationException("This operation is not available on the completed conversation");
    }

    /**
     * Loads a {@link CompletedConversation} from a data file.
     */
    public static CompletedConversation load(File file) throws IOException {
        return (CompletedConversation)new XmlFile(file).read();
    }

    /**
     * Saves the object to a data file.
     */
    public void save(File file) throws IOException {
        new XmlFile(file).write(this);
    }
}
