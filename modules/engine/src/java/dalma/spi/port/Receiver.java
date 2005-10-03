package dalma.spi.port;

/**
 * @author Kohsuke Kawaguchi
 */
interface Receiver<Key,Msg> {
    Key getKey();
    void handleMessage(Msg msg);
}
