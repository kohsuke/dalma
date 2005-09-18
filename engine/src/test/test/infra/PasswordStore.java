package test.infra;

import java.io.IOException;
import java.util.Properties;

/**
 * Gets password from a property file.
 * 
 * @author Kohsuke Kawaguchi
 */
public class PasswordStore {
    public static final String get(String key) {
        try {
            Properties props = new Properties();
            props.load(PasswordStore.class.getResourceAsStream("/passwords.properties"));
            return (String)props.get(key);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
