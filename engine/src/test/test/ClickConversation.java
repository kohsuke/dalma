package test;

import test.port.input.LineInputPort;

import java.io.Serializable;

/**
 * @author Kohsuke Kawaguchi
 */
public class ClickConversation implements Runnable, Serializable {
    private final int id;

    public ClickConversation(int id) {
        this.id = id;
    }
    public ClickConversation() {
        this(idGen++);
    }

    private static int idGen = 0;

    public void run() {
        out("started");
        loop(0);
        out("ended");
    }

    private void loop(int depth) {
        while(true) {
            out("current loop depth "+depth);
            String input = LineInputPort.waitForInput();
            if(input.length()>0) {
                loop(depth+1);
            } else {
                return;
            }
        }
    }

    private void out(String msg) {
        System.out.println(id+" "+msg);

    }
}
