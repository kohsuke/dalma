package dalma.webui;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.LogRecord;
import java.util.List;
import java.util.Collections;

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

    /**
     * Optional method to return log entries associated with this object, if any.
     * @param inclusive
     */
    public List<LogRecord> getLogs(boolean inclusive) {
        return Collections.emptyList();
    }

    public final List<LogRecord> getExclusiveLogs() {
        return getLogs(false);
    }
    public final List<LogRecord> getInclusiveLogs() {
        return getLogs(true);
    }

    protected final void sendError(StaplerRequest req, String msg, StaplerResponse resp) throws ServletException, IOException {
        req.setAttribute("message",msg);
        resp.forward(this,"error",req);
    }

    protected final void sendError(StaplerRequest req, Exception e, StaplerResponse resp) throws ServletException, IOException {
        req.setAttribute("message",e.getMessage());
        req.setAttribute("exception",e);
        resp.forward(this,"error",req);
    }
}
