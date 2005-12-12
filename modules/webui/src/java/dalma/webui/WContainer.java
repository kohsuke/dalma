package dalma.webui;

import dalma.container.Container;

/**
 * {@link Container} wrapper for the web UI.
 *
 * @author Kohsuke Kawaguchi
 */
public class WContainer {
    public final Container core;

    public WContainer(Container core) {
        this.core = core;
    }
}
