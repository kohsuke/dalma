import dalma.Program;
import dalma.Engine;
import dalma.Description;
import dalma.Resource;

/**
 * @author Kohsuke Kawaguchi
 */
@Description("this is a do-nothing workflow application")
public class Main extends Program {
    @Resource(description="description of resource X")
    public int x;
    @Resource(description="this is Y")
    public String y;

    public void main(Engine engine) throws Exception {
        System.out.println("Running");
        if(engine.getConversations().isEmpty()) {
            System.out.println("Started conversation");
            for( int i=0; i<10; i++ )
                engine.createConversation(new MyConversation());
        }
    }
}
