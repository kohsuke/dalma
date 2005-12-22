package dalma.webui;

import dalma.container.Container;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Entry point.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main implements ServletContextListener {

    private WContainer container;

    public void contextInitialized(ServletContextEvent event) {
        File home = getHomeDir(event);
        home.mkdirs();
        System.out.println("dalma home directory: "+home);

        LogRecorder logRecorder = new LogRecorder();
        Logger.getLogger("dalma").addHandler(logRecorder);

        try {
            container = new WContainer(Container.create(home), logRecorder);
            event.getServletContext().setAttribute("app",container);
        } catch (IOException e) {
            throw new Error(e);
        }

        // set the version
        Properties props = new Properties();
        try {
            InputStream is = getClass().getResourceAsStream("version.properties");
            if(is!=null)
                props.load(is);
        } catch (IOException e) {
            e.printStackTrace(); // if the version properties is missing, that's OK.
        }
        Object ver = props.get("version");
        if(ver==null)   ver="?";
        event.getServletContext().setAttribute("version",ver);
    }

    public void contextDestroyed(ServletContextEvent event) {
        if(container!=null)
            container.shutdown();
    }

    /**
     * Determines the home directory for Hudson.
     */
    private File getHomeDir(ServletContextEvent event) {
        // check JNDI for the home directory first
        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            String value = (String) env.lookup("DALMA_HOME");
            if(value!=null && value.trim().length()>0)
                return new File(value);
        } catch (NamingException e) {
            ; // ignore
        }

        // look at the env var next
        String env = EnvVars.masterEnvVars.get("HUDSON_HOME");
        if(env!=null)
            return new File(env);

        // otherwise pick a place by ourselves
        String root = event.getServletContext().getRealPath("/WEB-INF/workspace");
        if(root!=null)
            return new File(root);

        // if for some reason we can't put it within the webapp, use home directory.
        return new File(new File(System.getProperty("user.home")),".dalma");
    }
}
