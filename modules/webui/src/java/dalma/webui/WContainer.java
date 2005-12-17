package dalma.webui;

import dalma.container.Container;
import dalma.container.ContainerMBean;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * {@link Container} wrapper for the web UI.
 *
 * @author Kohsuke Kawaguchi
 */
public class WContainer implements UIObject {
    public final ContainerMBean core;

    public WContainer(ContainerMBean core) {
        this.core = core;
    }

    public String getDisplayName() {
        return "Dalma";
    }

    public boolean isUseSecurity() {
        return false; // TODO
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
            sendError(req, e.getMessage(), resp);
            return;
        }

        if(appName==null || contents==null || appName.length()==0 || contents.length==0) {
            sendError(req, "form data incomplete", resp);
            return;
        }

        core.deploy(appName,contents);

        resp.sendRedirect(req.getContextPath());
    }

    private void sendError(StaplerRequest req, String msg, StaplerResponse resp) throws ServletException, IOException {
        req.setAttribute("message",msg);
        resp.forward(this,"error",req);
    }
}
