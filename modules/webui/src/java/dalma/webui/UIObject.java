package dalma.webui;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class UIObject {
    /**
     * Returns the name used in the top breadcrumb.
     */
    public abstract String getDisplayName();

    /**
     * Returns the URL of this object.
     */
    public abstract String getUrl();

    protected final void sendError(StaplerRequest req, String msg, StaplerResponse resp) throws ServletException, IOException {
        req.setAttribute("message",msg);
        resp.forward(this,"error",req);
    }
}
