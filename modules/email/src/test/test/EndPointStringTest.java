package test;

import junit.framework.TestCase;
import dalma.Engine;
import dalma.EngineFactory;
import dalma.EndPoint;
import dalma.endpoints.email.EmailEndPoint;
import dalma.helpers.ThreadPoolExecutor;

import java.io.File;

/**
 * Tests the endpoint configuration via a connection string.
 * @author Kohsuke Kawaguchi
 */
public class EndPointStringTest extends TestCase {
    public void test1() throws Exception {
        Engine engine = EngineFactory.newInstance(
            new File("target/endpoint-string-test"),
            getClass().getClassLoader(),
            new ThreadPoolExecutor(3));
        EndPoint ep = engine.addEndPoint("mail", "smtp://dalma-test1@kohsuke.org!maildir://.");
        assertTrue(ep instanceof EmailEndPoint);
        engine.stop();
    }
}
