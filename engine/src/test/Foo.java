
/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) {
        synchronized(args) {
            bar();
        }
    }

    private static void bar() {
        throw new RuntimeException();
    }
}
