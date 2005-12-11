package dalma.container;

import dalma.helpers.Java5Executor;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Properties;
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

        Properties conf = loadProperties();

        Container container = new Container(home, new Java5Executor(
            Executors.newFixedThreadPool(readProperty(conf,"thread.count",5))));

        int jmxPort = readProperty(conf, "jmx.port", -1);
        if(jmxPort>=0) {
            logger.info("Initializing JMXMP connector at port "+jmxPort);
            JMXServiceURL url = new JMXServiceURL("jmxmp", null, jmxPort);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, ManagementFactory.getPlatformMBeanServer());

            cs.start();
            logger.info("Started JMXMP connector");
        }

        Thread.currentThread().suspend();

        //Thread.sleep(10*1000);
        //logger.info("Terminating");
        //container.stop();
        //logger.info("done");
    }

    private static int readProperty( Properties props, String key, int defaultValue ) {
        String value = props.getProperty(key);
        if(value==null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.severe("Configuration value for "+key+" must be int, but found \""+value+"\"");
            return defaultValue;
        }
    }
    private static Properties loadProperties() {
        Properties props = new Properties();
        File config = getConfigFile("dalma.properties");
        if(config.exists()) {
            try {
                FileInputStream in = new FileInputStream(config);
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE,"Failed to read "+config,e);
            }
        }
        return props;
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
