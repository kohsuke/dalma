package dalma.sample.hangman;

import dalma.Engine;
import dalma.EngineFactory;
import dalma.Program;
import dalma.Resource;
import dalma.EndPoint;
import dalma.endpoints.email.EmailEndPoint;
import dalma.endpoints.email.NewMailHandler;
import dalma.helpers.ThreadPoolExecutor;

import javax.mail.internet.MimeMessage;
import java.io.File;

/**
 * Entry point to the hangman game daemon.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main extends Program {

    /**
     * The {@link EndPoint} we use to connect to the e-mail system.
     */
    @Resource(description="e-mail configuration where hangman service is exposed")
    public EmailEndPoint email;

    /**
     * CLI entry point.
     *
     * This entry points hosts the dalma engine in itself
     * and keeps running until the process is killed externally.
     */
    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    private void run(String[] args) throws Exception {
        if(args.length!=1) {
            System.err.println("Usage: java -jar hangman.jar <e-mail endpointURL>");
            System.err.println();
            System.err.println("See https://dalma.dev.java.net/nonav/maven/dalma-endpoint-email/hangman.html for details");
            System.exit(-1);
        }

        // initialize the directory to which we store data
        File root = new File("hangman-data");
        root.mkdirs();

        // set up an engine.
        // we'll create one e-mail endpoint from the command-line.
        final Engine engine = EngineFactory.newEngine(root,new ThreadPoolExecutor(1,true));
        email = (EmailEndPoint)engine.addEndPoint("email", args[0]);
        init(engine);

        // start an engine
        engine.start();
        System.out.println("engine started and ready for action");

        // returning from the main thread means the engine will run forever.
        // it's somewhat like how Swing works.
    }

    /**
     * Performs initialization.
     *
     * <p>
     * When hosted inside a container, this method is invoked by the container.
     * See {@link Program} for more about how the container invokes this class.
     */
    @Override
    public void init(final Engine engine) {
        email.setNewMailHandler(new NewMailHandler() {
            /**
             * This method is invoked every time this endpoint receives a new e-mail.
             * Start a new game.
             */
            public void onNewMail(MimeMessage mail) throws Exception {
                System.out.println("Starting a new game for "+mail.getFrom()[0]);
                engine.createConversation(new HangmanWorkflow(email,mail));
            }
        });
    }
}
