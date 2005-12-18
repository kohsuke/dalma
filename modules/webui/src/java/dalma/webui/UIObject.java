package dalma.webui;

/**
 * @author Kohsuke Kawaguchi
 */
public interface UIObject {
    /**
     * Returns the name used in the top breadcrumb.
     */
    String getDisplayName();

    /**
     * Returns the URL of this object.
     */
    String getUrl();
}
