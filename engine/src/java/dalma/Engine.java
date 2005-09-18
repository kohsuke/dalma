package dalma;

import dalma.spi.port.EndPoint;

import java.util.Collection;
import java.util.Map;
import java.io.IOException;


/**
 * Workflow engine.
 *
 * TODO: think about the reconstruction of this. how should it work?
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
     */
    void addEndPoint(EndPoint endPoint);
}
