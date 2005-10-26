package test;

import dalma.test.Launcher;

import dalma.endpoints.input.LineInputEndPoint;

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

    protected void setUpEndPoints() throws Exception {
        engine.addEndPoint(new LineInputEndPoint());
    }

    protected void init() throws Exception {
        createConversation(ClickConversation.class);
        createConversation(ClickConversation.class);
    }
}
