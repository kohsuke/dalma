package dalma.webui;

import dalma.container.Container;

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
}
