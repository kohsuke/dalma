package test;

import dalma.Conversation;
import dalma.Engine;
import dalma.test.Launcher;
import dalma.helpers.ThreadPoolExecutor;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import org.apache.commons.javaflow.ContinuationClassLoader;
import dalma.test.MaskingClassLoader;
import dalma.test.Launcher;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Run conversations that wait for a keyboard input.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClickTest extends Launcher {
    public ClickTest(String[] args) throws Exception {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new ClickTest(args);
    }

    protected void init() throws Exception {
        createConversation(ClickConversation.class);
        createConversation(ClickConversation.class);
    }
}
