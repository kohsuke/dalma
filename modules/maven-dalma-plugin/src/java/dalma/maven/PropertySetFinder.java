package dalma.maven;

import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.JellyContext;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Used as a JellyBean to list up test properties from the context.
 *
 * @author Kohsuke Kawaguchi
 */
public class PropertySetFinder extends TagSupport {

    /**
     * Enumerate all properties with this prefix.
     */
    private String prefix;

    /**
     * Put the resulting {@link Map} into {@link JellyContext}
     * with this name.
     */
    private String var;

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setVar(String var) {
        this.var = var;
    }


    public void doTag(XMLOutput xmlOutput) throws MissingAttributeException, JellyTagException {
        Map r = new HashMap();
        for( JellyContext context = getContext(); context!=null; context=context.getParent() ) {
            for (Iterator itr = context.getVariables().entrySet().iterator(); itr.hasNext();) {
                Map.Entry e = (Map.Entry) itr.next();
                String key = (String)e.getKey();
                if(key.startsWith(prefix)) {
                    if(!r.containsKey(key)) {
                        r.put(e.getKey(),e.getValue());
                    }
                }
            }
        }

        getContext().setVariable(var,r);
    }

}
