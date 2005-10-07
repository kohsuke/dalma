package test.spring;

import dalma.EngineFactory;
import dalma.Engine;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import junit.framework.TestCase;

/**
 * Spring configuration test.
 * @author Kohsuke Kawaguchi
 */
public class SpringTest extends TestCase {
    public void test1() throws Exception {
        XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("config.xml",SpringTest.class));
        EngineFactory ef = (EngineFactory)xbf.getBean("engine");
        Engine engine = ef.newInstance();
        assertNotNull(engine.getEndPoint("e-mail"));
        engine.stop();
    }
}
