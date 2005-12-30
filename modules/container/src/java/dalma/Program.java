package dalma;

import java.util.logging.Logger;

/**
 * Entry point of the user-implemented workflow application.
 *
 * <p>
 * A workflow application that runs inside the dalma container
 * needs to have a class that derives from this class. Such class
 * serves as the entry point to the workflow program, and its fully-qualified
 * class name must be <tt>Main</tt> (in the root package.)
 *
 * <h3>Configuring Program</h3>
 * <p>
 * A {@link Program} typically needs to be configured with environment-specific
 * information to function. For example, perhaps a program is some kind of e-mail
 * automation system and may use 2 e-mail endpoints and 1 IRC endpoint.
 *
 * A {@link Program} communicates these configuration requirements to the container
 * by using {@link Resource} annotation. In the above example, the {@link Program}
 * would be written something like:
 *
 * <pre>
 * class Main extends dalma.Program {
 *   &#x40;Resource
 *   public EmailEndPoint externalMail;
 *
 *   &#x40;Resource
 *   public EmailEndPoint maintananceMail;
 *
 *   &#x40;Resource
 *   public IRCEndPoint chatEndPoint;
 *
 *   &#x40;Resource
 *   public String greetingMessage;
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * At runtime, the container invokes setters and sets fields to "inject" references
 * into the program before the {@link #init(Engine)} method
 * is invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Program {
    /**
     * Called right after the {@link Engine} instance is created
     * and populated with the configured endpoints.
     *
     * <p>
     * This callback can be used to further configure endpoints,
     * for example by installing listeners and etc.
     *
     * <p>
     * In rare case, when a {@link Program} doesn't know statically
     * what endpoints it wants to use,
     * this callback can be also used to add additional endpoints
     * programatically if desired.
     *
     * <p>
     * Note that if kinds and endpoints are known statically,
     * then the {@link Program}-derived class can use a resource injection
     * to get access to those endpoints.
     *
     * @throws Exception
     *      Any exception thrown by this method is considered to indicate
     *      an error, and prevents the {@link Program} from running.
     */
    public void init(Engine engine) throws Exception {
        // noop
    }

    /**
     * Called after the {@link Engine} is {@link Engine#start()} started.
     *
     * <p>
     * In rare case, when a {@link Program} wants to start a new
     * {@link Conversation} proactively, it can use this callback to do so.
     *
     * @throws Exception
     *      Any exception thrown by this method is considered to indicate
     *      an error, and prevents the {@link Program} from running.
     */
    public void main(Engine engine) throws Exception {
        // noop
    }

    /**
     * Called right before the application is shut down,
     * to perform any optional clean up.
     *
     * @throws Exception
     *      Any exception thrown by this method is recorded
     *      but otherwise the shut down operation continues regardless. 
     */
    public void cleanup(Engine engine) throws Exception {

    }

    // start with the default logger instance so that the user program
    // can start using it even within the constructor.
    // we'll replace this with a real logger at some later stage
    // of initialization
    private Logger logger = Logger.getLogger(Program.class.getName());

    /**
     * Gets the logger for this workflow application.
     *
     * <p>
     * Logs recorded by this logger will be made available
     * to the monitoring/management applications. 
     *
     * @return
     *      always non-null.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Reserved for the container. Do not invoke this method.
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
