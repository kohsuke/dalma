package dalma.impl.port.filemonitor;

/**
 * Monitors a directory and checks for new files.
 *
 * TODO: this is written as a test.
 *
 * @author Kohsuke Kawaguchi
 */
//public class FileMonitorPort implements EndPoint, Serializable {
//
//    private static final FileMonitorPort INSTANCE = new FileMonitorPort();
//
//    /**
//     * Monitored files.
//     */
//    private static final Map<ConversationSPI,File> files = new HashMap<ConversationSPI,File>();
//
//    private FileMonitorPort() {
//        new Thread() {
//            public void run() {
//                try {
//                    while(true) {
//                        check();
//                        Thread.sleep(3000);
//                    }
//                } catch (InterruptedException e) {
//                    // this is a signal to die
//                }
//            }
//        }.start();
//    }
//
//    public void onRemoved(ConversationSPI conv) {
//        synchronized(files) {
//            files.remove(conv);
//        }
//    }
//
//    private static void check() {
//        synchronized(files) {
//            for (Map.Entry<ConversationSPI, File> e : files.entrySet()) {
//                File f = e.getValue();
//                if (f.exists()) {
//                    e.getKey().resume(null);
//                }
//            }
//        }
//    }
//
//    /**
//     * Blocks until there's a file specified by the given name.
//     */
//    public static void waitForFile(final File f) {
//        final ConversationSPI cnv = ConversationSPI.getCurrentConversation();
//
//        cnv.scheduleFollowUp(new Runnable() {
//            public void run() {
//                synchronized(files) {
//                    files.put(cnv,f);
//                }
//            }
//        });
//        cnv.suspend(INSTANCE);
//    }
//
//    private Object readResolve() {
//        return INSTANCE;
//    }
//
//    private static final long serialVersionUID = 1L;
//}
