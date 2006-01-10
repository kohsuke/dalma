package dalma.webui;

import dalma.Conversation;

import java.util.Date;
import java.util.List;
import java.util.logging.LogRecord;

/**
 * @author Kohsuke Kawaguchi
 */
public class WConversation extends UIObject {
    private final Conversation core;
    private final WWorkflow parent;

    public static WConversation wrap(WWorkflow parent,Conversation conv) {
        if(conv==null)   return null;
        return new WConversation(parent,conv);
    }

    private WConversation(WWorkflow parent,Conversation core) {
        this.parent = parent;
        this.core = core;
    }

    public WWorkflow getParent() {
        return parent;
    }

    public int getId() {
        return core.getId();
    }

    public String getDisplayName() {
        return "#"+core.getId();
    }

    public String getTitle() {
        return core.getTitle();
    }

    public String getUrl() {
        return parent.getUrl()+"conversation/"+core.getId()+'/';
    }

    public Date getStartDate() {
        return core.getStartDate();
    }

    public List<LogRecord> getLogs(boolean inclusive) {
        return core.getLog();
    }
}
