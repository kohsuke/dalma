package dalma;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.File;
import java.text.ParseException;


/**
 * Workflow engine.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Engine {
    // at the engine level, it makes sense to separate class loading from the engine.
//    /**
//     * Creates a new {@link Workflow} by using the specified jar files
//     * as classpath.
//     */
//    Workflow createWorkflow( String entryPointClassName, URL[] classpath );
//
//    /**
//     * Creates a new {@link Workflow} by using the specified entry
//     * point class that derives from {@link Program} interface.
//     *
//     * <p>
//     * This is for advanced use cases where the caller is responsible
//     * for the javaflow bytecode enhancement of the classes.
//     */
//    Workflow createWorkflow( Class entryPoint );

    // TODO: ad-hoc conversation?
    // wouldn't it be nice if we can start a new workflow by simply
    // passing an instance of a Program?

    Conversation createConversation( Runnable target ) throws IOException;

    /**
     * Returns the list of {@link Conversation}s in this engine.
     *
     * @return
     *      always return non-null collection. The returned object
     *      is a snapshot of the conversations.
     */
    Collection<Conversation> getConversations();
    // snapshot, because of the synchronization issue

    /**
     * Gets a read-only copy of all the {@link EndPoint}s in this engine.
     *
     * @return
     *      always retrun non-null (but possibly empty) collection.
     */
    Map<String,EndPoint> getEndPoints();

    /**
     * Gets the {@link EndPoint} of the given name.
     *
     * @return
     *      null if no such {@link EndPoint} is found.
     */
    EndPoint getEndPoint(String name);

    /**
     * Adds a new {@link EndPoint} to this engine.
     *
     * @throws IllegalArgumentException
     *      if there's already an {@link EndPoint} that has the same name.
     * @throws IllegalStateException
     *      if the engine is already started.
     */
    void addEndPoint(EndPoint endPoint);

    /**
     * Creates and adds a new {@link EndPoint} to this engine.
     *
     * <p>
     * See <a href="https://dalma.dev.java.net/nonav/maven/endpointURL.html">
     * this document for details</a>.
     *
     * @param endPointName
     *      name of the endpoint. this will become the value
     *      returned from {@link EndPoint#getName()}. Must not be null.
     * @param endpointURL
     *      configuration of an endpoint encoded in an URI form.
     *      must not be null.
     * @throws ParseException
     *      if there's an error in the connection string.
     * @throws IllegalArgumentException
     *      if there's already an {@link EndPoint} that has the same name.
     * @throws IllegalStateException
     *      if the engine is already started.
     * @return
     *      the endpoint created from the connection string.
     */
    EndPoint addEndPoint(String endPointName, String endpointURL) throws ParseException;

    /**
     * Adds multiple {@link EndPoint}s to this engine at once.
     *
     * <p>
     * Suppose you have a property file like this:
     * <pre>
     * email1=smtp://hangman@kohsuke.org!pop3://username:password@mail.kohsuke.org
     * email2=smtp://oracle@kohsuke.org!pop3://username:password@mail.kohsuke.org
     * </pre>
     * <p>
     * You can then read this file into {@link Properties}, and then pass that
     * into this method to add two {@link EndPoint}s with one method call.
     * <p>
     * This is convenient when you are externalizing the endpoint configuration
     * in a property file.
     *
     * @param endpointURLs
     *      {@link Properties} that has the endpoint name as a key and
     *      endpoint URL as a value. Can be empty but must not be null.
     * @return
     *      a map that contains the newly created {@link EndPoint}s keyed by their names.
     * @throws ParseException
     *      if Dalma fails to parse any of the endpoint URLs.
     * @throws IllegalStateException
     *      if the engine is already started.
     */
    Map<String,EndPoint> addEndPoints(Properties endpointURLs) throws ParseException;

    /**
     * Configures an engine by using <a href="http://jakarta.apache.org/bsf/">Bean Scripting Framework</a>.
     *
     * <p>
     * This method is intended to run a script that configures endpoints.
     * By moving the endpoint configuration to a script, you can allow it
     * to be changed at runtime.
     *
     * <p>
     * For this method to work, you need to have:
     * <ol>
     *  <li><tt>bsf.jar</tt> in your classpath
     *  <li>scripting language engine that you use in your classpath
     *      (for example, if you use BeanShell, you need <tt>bsh.jar</tt>)
     * </ol>
     *
     * <p>
     * The file extension is used to determine the scripting language engine.
     * For the list of languages available out-of-box with BSF and their
     * registered file extensions, see
     * <a href="http://svn.apache.org/viewcvs.cgi/jakarta/bsf/trunk/src/org/apache/bsf/Languages.properties?view=markup">this document</a>.
     * For example, beanshell is ".bsh", groovy is ".groovy", JavaScript is ".js".
     *
     * <p>
     * The {@link Engine} object is made available to the script with the name 'engine'.
     *
     * @param scriptFile
     *      The file that contains the script to be run. Must not be null.
     * @see http://dalma.dev.java.net/nonav/maven/configure.html#Configuring_with_Bean_Scripting_Framework
     */
    void configureWithBSF(File scriptFile) throws IOException;

    /**
     * Starts the engine and activates all the {@link EndPoint}s.
     *
     * <p>
     * This method must be called after all the {@link EndPoint}s are
     * added to the engine, and before a conversation is submitted.
     *
     * <p>
     * This method may throw an exception if any of the dalma components
     * (such as the engine and endpoints) fail to start correctly.
     *
     * @throws IllegalStateException
     *      if the engin has already been started.
     */
    void start();

    /**
     * Stops the engine and releases all the resources it acquired.
     *
     * <p>
     * This method blocks until all the running {@link Conversation} suspends/completes,
     * so it may take some time.
     *
     * @throws InterruptedException
     *      if the calling thread is interrupted while waiting for the completion. 
     */
    void stop() throws InterruptedException;

    /**
     * Sets the logger that this engine uses.
     *
     * @param logger
     *      if null, the engine will stop logging.
     */
    void setLogger(Logger logger);

    /**
     * Waits until all the conversation in the engine exits.
     */
    void waitForCompletion() throws InterruptedException;

    /**
     * Checks if conversations in the engine had any fatal error.
     *
     * <p>
     * If a conversation in this engine dies by throwing an {@link Error}
     * or {@link RuntimeException}, the engine puts such exception in
     * the 'error queue' and kills that conversation.
     *
     * <p>
     * Applications can invoke this method to check the error queue.
     * If there's any error in the queue, this method throws it as
     * an {@link Error} or {@link RuntimeException}, respectively.
     *
     * <p>
     * If the error queue is empty, this method simply returns without
     * blocking.
     */
    void checkError();
}
