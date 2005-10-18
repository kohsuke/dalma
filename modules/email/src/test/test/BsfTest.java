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
        String fileName = url.substring("file:".length());
        while(fileName.startsWith("/"))
            fileName = fileName.substring(1);
        if(File.pathSeparatorChar==':')
            fileName = '/'+fileName;    // on Unix
        String file = URLDecoder.decode(fileName,"UTF-8");
        engine.configureWithBSF(new File(file));
    }

    public void test() throws Throwable {
        assertTrue(engine.getEndPoint("ep-name")!=null);
    }
}
