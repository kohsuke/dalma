import dalma.Program;
import dalma.Engine;
import dalma.Conversation;
import dalma.Resource;
import dalma.endpoints.invm.Channel;

import java.io.IOException;

import junit.framework.Assert;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main extends Program {
    @Override
    public void init(Engine engine) {
        System.out.println("init");
    }

    @Resource
    public int n;

    @Resource
    public void setFoo(String value) {
        System.out.println("Resource injected: "+value);
        Assert.assertEquals("str",value);
    }

    @Override
    public void main(Engine engine) throws IOException, InterruptedException {
        System.out.println("main");

        Assert.assertEquals(n,10);

        Channel[] channels = new Channel[10];
        for( int i=0; i<channels.length; i++ )
            channels[i] = new Channel();

        //// this doesn't involve any suspension
        //System.out.println("Self loopback test");
        //Conversation conv = engine.createConversation(new Alice(channels[0], channels[0]));
        //conv.join();
        //System.out.println("done");

        System.out.println("Pingpong test");
        Conversation conv1 = engine.createConversation(new Alice(channels[0],channels[3]));
        Conversation conv2 = engine.createConversation(new Alice(channels[1],channels[3]));
        new Thread(new Bob(channels[3])).start();
        conv1.join();
        conv2.join();
        System.out.println("done");
    }

    @Override
    public void cleanup(Engine engine) {
        System.out.println("cleanup");
    }
}
