package dalma.webui;

import java.util.Map;

/**
 * Access to the environmental variables.
 * 
 * @author Kohsuke Kawaguchi
 */
class EnvVars {
    /**
     * Environmental variables that we've inherited.
     */
    public static final Map<String,String> masterEnvVars = System.getenv();
}
