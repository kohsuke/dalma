package dalma.container;

import dalma.helpers.Java5Executor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Entry point to the dalma container.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    // run it with -Dcom.sun.management.jmxremote=true to enable JMX monitoring
    public static void main(String[] args) throws Exception {
        File home = getHome();

        logger.info("Starting dalma container with DALMA_HOME="+home);

        Container container = new Container(home, new Java5Executor(Executors.newFixedThreadPool(5)));
        container.enableAutoRedeploy();

        Thread.currentThread().suspend();

        //Thread.sleep(10*1000);
        //logger.info("Terminating");
        //container.stop();
        //logger.info("done");
    }

    /**
     * Gets the dalma container home directory.
     */
    public static File getHome() {
        String home = System.getProperty("DALMA_HOME");
        if(home!=null)
            return new File(home);

        return new File(".");
    }

    static {
        // configure the logger
        configureLogger();
    }

    private static void configureLogger() {
        // this is the default
        Logger.getLogger("dalma").setLevel(Level.ALL);

        // if the property file exists, load that configuration
        File logProperties = new File(getHome(),"logging.properties");
        if(logProperties.exists()) {
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(logProperties));
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Failed to read "+logProperties,e);
            }
        }
    }
}
