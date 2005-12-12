package dalma.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

        Container container = Container.create(home);

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

    public static File getConfigFile(String name) {
        return new File(new File(getHome(),"conf"),name);
    }

    static {
        // configure the logger
        configureLogger();
    }

    private static void configureLogger() {
        // this is the default
        Logger.getLogger("dalma").setLevel(Level.ALL);

        // if the property file exists, load that configuration
        File logProperties = getConfigFile("logging.properties");
        if(logProperties.exists()) {
            try {
                FileInputStream in = new FileInputStream(logProperties);
                try {
                    LogManager.getLogManager().readConfiguration(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Failed to read "+logProperties,e);
            }
        }
    }
}
