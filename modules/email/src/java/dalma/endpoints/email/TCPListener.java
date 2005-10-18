package dalma.endpoints.email;

import dalma.DalmaException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Listener} that picks up messages from a TCP connection.
 *
 * <p>
 * This listener listens to a particular TCP port and waits for clients
 * to send e-mails through connections. One connection is used to send
 * one e-mail message, complete from its headers to body.
 *
 * <p>
 * Combined with modern MTAs that can invoke a program upon a recipt
 * of an e-mail (like qmail, exim, etc), and telnet, this can be used
 * to deliver e-mails to a running dalma engine without polling.
 *
 * <p>
 * For example, with the following <tt>/var/qmail/alias/.qmail-dalma</tt>
 * or <tt>~/.forward</tt> file,
 * qmail/exim would deliver newly received e-mails to the running
 * dalma engine through a {@link TCPListener}, assuming that it's listening
 * on the port 19550.
 * <pre>
 * | /bin/telnet localhost 19550
 * </pre>
 *
 * <p>
 * Doing this with sendmail requires tinkering with <tt>/etc/aliases</tt>.
 *
 * @author Kohsuke Kawaguchi
 */
public class TCPListener extends Listener {
    private final Thread thread;

    private InetSocketAddress address;

    /**
     * Once the TCP port is open, set to non-null.
     */
    private Selector selector = null;

    private static final Logger logger = Logger.getLogger(TCPListener.class.getName());

    public TCPListener(int port) {
        this(getLoopbackAddress(),port);
    }

    public TCPListener(InetAddress listenAddress, int port) {
        this(new InetSocketAddress(listenAddress,port));
    }
    public TCPListener(InetSocketAddress adrs) {
        thread = new Thread(new Runner());
        this.address = adrs;
    }

    protected void start() {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(address);

            selector = SelectorProvider.provider().openSelector();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new DalmaException(e);
        }

        thread.start();
    }

    protected void stop() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // process the interruption later
            Thread.currentThread().interrupt();
        }

        // close any open connection
        try {
            for (SelectionKey sk : selector.keys())
                sk.channel().close();
            selector.close();
        } catch (IOException e) {
            throw new DalmaException(e);
        }
    }

    private class Runner implements Runnable {
        public void run() {
            while (true) {
                try {
                    selector.select();
                } catch (IOException e) {
                    // is this error recoverable?
                    logger.log(Level.SEVERE,"failed to select a socket",e);
                    return;
                }

                // Someone is ready for I/O, get the ready keys
                Set<SelectionKey> readyKeys = selector.selectedKeys();

                // Walk through the ready keys collection and process date requests.
                for( SelectionKey sk : readyKeys ) {
                    // The key indexes into the selector so you
                    // can retrieve the socket that's ready for I/O

                    if(sk.isAcceptable()) {
                        // accept this connection
                        try {
                            ServerSocketChannel newConnection = (ServerSocketChannel)sk.channel();
                            SocketChannel ncs = newConnection.accept();
                            ncs.configureBlocking(false);
                            ncs.register(selector,SelectionKey.OP_READ,new ReceivedMessage());
                        } catch (IOException e) {
                            logger.log(Level.WARNING,"failed to accept a connection",e);
                        }
                    } else
                    if(sk.isReadable()) {
                        SocketChannel client = (SocketChannel) sk.channel();
                        try {
                            ReceivedMessage msg = (ReceivedMessage) sk.attachment();
                            msg.read(client);
                        } catch (IOException e) {
                            logger.log(Level.WARNING,"failed to read from a socket",e);
                            // TODO: shall we close this client?
                        }
                    }
                }

                readyKeys.clear();

                if(Thread.interrupted()) {
                    // if interrupted, die now
                    return;
                }
            }
        }
    }

    private static InetAddress getLoopbackAddress() {
        try {
            return InetAddress.getAllByName(null)[0];
        } catch (UnknownHostException e) {
            throw new Error(e); // impossible
        }
    }

    /**
     * Represents a partially received message.
     */
    private class ReceivedMessage {
        /**
         * Accumulated data.
         */
        ByteBuffer buf = ByteBuffer.allocate(4096);

        void read(SocketChannel channel) throws IOException {
            int r = channel.read(buf);
            if(r<0) {
                buf.flip();
                finish();
                channel.close();
                return;
            }

            if(buf.remaining()==0) {
                // saturated the buffer. get a bigger one
                ByteBuffer newBuf = ByteBuffer.allocate(buf.capacity() * 2);
                buf.flip();
                newBuf.put(buf);
                buf = newBuf;
            }
        }

        /**
         * Called when a message is completely read.
         */
        private void finish() {
            InputStream is = new InputStream() {
                public int read(byte b[], int off, int len) {
                    if(buf.hasRemaining()) {
                        len = Math.min(len, buf.remaining());
                        buf.get(b,off,len);
                        return len;
                    } else {
                        return -1;
                    }
                }

                public int read() {
                    if(buf.hasRemaining())
                        return buf.get();
                    else
                        return -1;
                }
            };
            try {
                MimeMessage msg = new MimeMessage(getEndPoint().getSession(),is);
                handleMessage(msg);
            } catch (MessagingException e) {
                // this happens when a client sends us something other than e-mail
                logger.log(Level.WARNING,"failed to parse into a message",e);
            }
        }
    }
}
