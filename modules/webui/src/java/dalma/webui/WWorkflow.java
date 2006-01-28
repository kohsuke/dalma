package dalma.webui;

import dalma.Engine;
import dalma.Conversation;
import dalma.container.FailedOperationException;
import dalma.container.WorkflowApplication;
import dalma.container.WorkflowState;
import dalma.container.model.Model;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.LogRecord;

/**
 * @author Kohsuke Kawaguchi
 */
public class WWorkflow extends UIObject implements Comparable<WWorkflow> {
    private final WorkflowApplication core;

    public WWorkflow(WorkflowApplication core) {
        this.core = core;
    }

    public String getName() {
        return core.getName();
    }

    public String getDisplayName() {
        return core.getName();
    }

    public String getDescription() {
        return core.getDescription();
    }

    public boolean isRunning () {
        return core.getState()==WorkflowState.RUNNING;
    }

    public String getUrl() {
        return "workflow/"+getName()+'/';
    }

    public Model getModel() {
        return core.getModel();
    }

    public boolean isConfigured() {
        return core.isConfigured();
    }

    public List<LogRecord> getLogs(boolean inclusive) {
        return core.getLogs(inclusive);
    }

    public String getConversationSize() {
        Engine engine = core.getEngine();
        return engine!=null ? String.valueOf(engine.getConversationsSize()) : "N/A";
    }

    public String getLastActiveTime() {
        Engine engine = core.getEngine();
        if(engine==null)    return "N/A";
        long t = engine.getLastActiveTime().getTime();
        if(t==0)            return "N/A";

        return Functions.getTimeSpanString(System.currentTimeMillis()-t);
    }

    /**
     * Has to be named as "get" to make JSTL happy. Ugly.
     */
    public Properties getConfigProperties() throws IOException {
        return core.loadConfigProperties();
    }


    public static WWorkflow wrap(WorkflowApplication app) {
        if(app==null)   return null;
        return new WWorkflow(app);
    }

    public WConversation getConversation(int id) {
        Engine e = core.getEngine();
        if(e!=null) {
            Conversation c = e.getConversation(id);
            if(c!=null)
                return WConversation.wrap(this,c);
        }

        return WConversation.wrap(this,core.getCompletedConversations().get(id));
    }

    public String getLogRotationDays() {
        int d = core.getDaysToKeepLog();
        if(d==-1)       return "";
        return String.valueOf(d);
    }

    public Collection<Conversation> getConversations() {
        Engine e = core.getEngine();
        if(e==null) return Collections.emptyList();
        return toSortedList(e.getConversations());
    }

    public Collection<Conversation> getCompletedConversations() {
        return toSortedList(core.getCompletedConversations().values());
    }

    private List<Conversation> toSortedList(Collection<Conversation> c) {
        List<Conversation> r = new ArrayList<Conversation>(c);
        Collections.sort(r,REVERSE_CONVERSATION_SORTER);
        return r;
    }

    public void doStop(StaplerRequest req, StaplerResponse resp) throws IOException {
        core.stop();
        resp.sendRedirect(".");
    }

    public void doStart(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        try {
            core.start();
            resp.sendRedirect(".");
        } catch (FailedOperationException e) {
            sendError(req,e,resp);
        }
    }

    public void doDoDelete(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        try {
            core.undeploy();
            resp.sendRedirect(req.getContextPath());
        } catch (FailedOperationException e) {
            sendError(req,e,resp);
        }
    }

    /**
     * Accepts the configuration page submission.
     */
    public void doPostConfigure(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        // TODO: report failed start operation correctly
        Properties props = core.loadConfigProperties();
        for( Map.Entry<String,String[]> e : ((Map<String,String[]>)req.getParameterMap()).entrySet() ) {
            String name = e.getKey();
            if(!name.startsWith("config-"))
                continue;
            name = name.substring(7);
            String value = e.getValue()[0];
            if(value.length()==0)
                props.remove(name);
            else
                props.put(name,value);
        }
        core.saveConfigProperties(props);

        String logRotateDays = req.getParameter("logrotate_days");
        if(logRotateDays==null || logRotateDays.length()==0) logRotateDays = "-1";
        try {
            core.setDaysToKeepLog(Integer.valueOf(logRotateDays));
        } catch(NumberFormatException e) {
            sendError(req,logRotateDays+" is not an integer",resp);
        }

        resp.sendRedirect(".");
    }

    public void doSubmitNewBinary(StaplerRequest req, StaplerResponse resp ) throws IOException, ServletException {
        byte[] contents = null;

        try {
            DiskFileUpload fu = new DiskFileUpload();
            for( FileItem fi : (List<FileItem>)fu.parseRequest(req) ) {
                if(fi.getFieldName().equals("file"))
                contents = fi.get();
            }
        } catch (FileUploadException e) {
            sendError(req, e, resp);
            return;
        }

        if(contents == null || contents.length==0) {
            sendError(req, "form data incomplete", resp);
            return;
        }

        try {
            core.owner.deploy(core.getName(),contents);
            resp.sendRedirect(".");
        } catch (FailedOperationException e) {
            sendError(req, e, resp);
        } catch (InterruptedException e) {
            sendError(req, e, resp);
        }
    }

    public int compareTo(WWorkflow that) {
        return this.getName().compareTo(that.getName());
    }

    private static final Comparator<Conversation> REVERSE_CONVERSATION_SORTER =
        new Comparator<Conversation>() {
            public int compare(Conversation lhs, Conversation rhs) {
                return rhs.getId()-lhs.getId();
            }
        };
}
