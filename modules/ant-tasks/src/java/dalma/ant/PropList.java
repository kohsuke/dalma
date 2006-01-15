package dalma.ant;

import org.apache.tools.ant.types.Environment;

import java.util.Properties;
import java.util.Vector;
import java.util.Map;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
final class PropList extends Environment {
    public Properties toProperties() {
        Properties r = new Properties();
        for (Variable v : (Vector<Variable>)variables) {
            r.put(v.getKey(),v.getValue());
        }
        return r;
    }

    public void addPropertyFile(File propFile) throws IOException {
        Properties props = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(propFile));
        try {
            props.load(in);
            for (Map.Entry<Object, Object> e : props.entrySet()) {
                Variable v = new Variable();
                v.setKey(e.getKey().toString());
                v.setValue(e.getValue().toString());
                variables.add(v);
            }
        } finally {
            in.close();
        }
    }
}
