package test;

import dalma.test.WorkflowTestProgram;

import java.io.File;
import java.net.URLDecoder;

/**
 * Tests BSF support.
 *
 * @author Kohsuke Kawaguchi
 */
public class BsfTest extends WorkflowTestProgram {
    public BsfTest(String name) {
        super(name);
    }

    protected void setupEndPoints() throws Exception {
        String url = getClass().getResource("test.bsh").toExternalForm();
        String file = URLDecoder.decode(url.substring("file:/".length()),"UTF-8");
        engine.configureWithBSF(new File(file));
    }

    public void test() throws Throwable {
        assertTrue(engine.getEndPoint("ep-name")!=null);
    }
}
