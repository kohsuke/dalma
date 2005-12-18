import dalma.Program;
import dalma.Engine;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main extends Program {
    public void main(Engine engine) throws Exception {
        for( int i=0; i<10; i++ )
            System.out.println("Running");
    }

    public String getDescription() {
        return "this is a do-nothing workflow application";
    }
}
