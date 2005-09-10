package test;

import dalma.Conversation;
import dalma.Engine;
import dalma.helpers.ThreadPoolExecutor;
import dalma.impl.EngineImpl;
import dalma.impl.Util;
import org.apache.commons.javaflow.ContinuationClassLoader;
import test.infra.MaskingClassLoader;
import test.infra.Launcher;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Run conversations that wait for a keyboard input.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClickTest {
    public static void main(String[] args) throws Exception {
        Launcher.main(ClickConversation.class, ClickConversation.class);
    }
}
