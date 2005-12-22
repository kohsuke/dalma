import dalma.Program;
import dalma.Engine;
import dalma.Description;

/**
 * @author Kohsuke Kawaguchi
 */
@Description("this is a do-nothing workflow application")
public class Main extends Program {
    public void main(Engine engine) throws Exception {
        for( int i=0; i<10; i++ )
            System.out.println("Running");
    }
}
