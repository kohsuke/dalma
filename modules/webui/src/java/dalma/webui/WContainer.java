package dalma.webui;

import dalma.container.Container;
import dalma.container.FailedOperationException;
import dalma.container.WorkflowApplication;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * {@link Container} wrapper for the web UI.
 *
 * @author Kohsuke Kawaguchi
 */
public class WContainer extends UIObject {
    public final Container core;

    private final LogRecorder logRecorder;

    public WContainer(Container core, LogRecorder logRecorder) {
        this.core = core;
        this.logRecorder = logRecorder;
    }

    public String getDisplayName() {
        return "Dalma";
    }

    public String getUrl() {
        return "";
    }

    public boolean isUseSecurity() {
        return false; // TODO
    }

    public List<LogRecord> getLogs() {
        return logRecorder.getLogRecords();
    }

    public void doCreateApp(StaplerRequest req, StaplerResponse resp ) throws IOException, ServletException {
        String appName = null;
        byte[] contents = null;

        try {
            DiskFileUpload fu = new DiskFileUpload();
            for( FileItem fi : (List<FileItem>)fu.parseRequest(req) ) {
                if(fi.isFormField()) {
                    if(fi.getFieldName()!=null && fi.getFieldName().equals("name"))
                        appName = fi.getString();
                } else {
                    contents = fi.get();
                }
            }
        } catch (FileUploadException e) {
            sendError(req, e, resp);
            return;
        }

        if(appName==null || contents==null || appName.length()==0 || contents.length==0) {
            sendError(req, "form data incomplete", resp);
            return;
        }

        try {
            WorkflowApplication wa = core.deploy(appName, contents);
            resp.sendRedirect(req.getContextPath()+'/'+WWorkflow.wrap(wa).getUrl());
        } catch (FailedOperationException e) {
            sendError(req, e, resp);
        } catch (InterruptedException e) {
            sendError(req, e, resp);
        }
    }

    public List<WWorkflow> getWorkflows() {
        List<WWorkflow> r = new ArrayList<WWorkflow>();
        for (WorkflowApplication a : core.getApplications()) {
            r.add(new WWorkflow(a));
        }
        return r;
    }

    public WWorkflow getWorkflow(String name) {
        return WWorkflow.wrap(core.getApplication(name));
    }

    public void shutdown() {
        Logger.getLogger("dalma").removeHandler(logRecorder);
        core.stop();
    }
}
