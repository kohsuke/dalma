package test;

import dalma.test.WorkflowTestProgram;
import junit.textui.TestRunner;

/**
 * Makes sure that endpoint name collisions are detected.
 * @author Kohsuke Kawaguchi
 */
public class EndPointNameCollisionTest extends WorkflowTestProgram {

    public EndPointNameCollisionTest(String name) {
        super(name);
    }

    public static void main(String[] args) throws Exception {
        TestRunner.run(EndPointNameCollisionTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        engine.addEndPoint("email",getProperty("email.endpoint1"));
        try {
            engine.addEndPoint("email",getProperty("email.endpoint2"));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("email"));
        }
    }

    public void test() throws Throwable {
    }
}
