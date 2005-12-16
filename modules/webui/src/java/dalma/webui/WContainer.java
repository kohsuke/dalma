package dalma.webui;

import dalma.container.Container;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * {@link Container} wrapper for the web UI.
 *
 * @author Kohsuke Kawaguchi
 */
public class WContainer implements UIObject {
    public final Container core;

    public WContainer(Container core) {
        this.core = core;
    }

    public String getDisplayName() {
        return "Dalma";
    }

    public boolean isUseSecurity() {
        return false; // TODO
    }

    public void doCreateApp(StaplerRequest req, StaplerResponse resp ) throws IOException, ServletException {
        resp.sendRedirect(req.getContextPath());
    }
}
