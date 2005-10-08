package dalma.endpoints.jms.impl;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Serializable} {@link MapMessage}.
 *
 * @author Kohsuke Kawaguchi
 */
public class MapMessageImpl extends MessageImpl implements MapMessage {

    private Map<String,Object> data = new HashMap<String,Object>();

    public MapMessageImpl() {
    }

    public MapMessageImpl(MapMessage s) throws JMSException {
        super(s);
        Enumeration e = s.getMapNames();
        while(e.hasMoreElements()) {
            String key = (String) e.nextElement();;
            data.put(key,s.getObject(key));
        }
    }

    public void clearBody() throws JMSException {
        data.clear();
    }

    public Enumeration getMapNames() throws JMSException {
        return Collections.enumeration(data.keySet());
    }

    public boolean itemExists(String name) throws JMSException {
        return data.containsKey(name);
    }

    public boolean getBoolean(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asBoolean(v);
    }

    public byte getByte(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asByte(v);
    }

    public short getShort(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asShort(v);
    }

    public char getChar(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asChar(v);
    }

    public int getInt(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asInt(v);
    }

    public long getLong(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asLong(v);
    }

    public float getFloat(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asFloat(v);
    }

    public double getDouble(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asDouble(v);
    }

    public String getString(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asString(v);
    }

    public byte[] getBytes(String name) throws JMSException {
        Object v = getObject(name);
        return Converter.get(v).asByteArray(v);
    }

    public Object getObject(String name) throws JMSException {
        return data.get(name);
    }

    public void setBoolean(String name, boolean value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setByte(String name, byte value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setShort(String name, short value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setChar(String name, char value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setInt(String name, int value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setLong(String name, long value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setFloat(String name, float value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setDouble(String name, double value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setString(String name, String value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setBytes(String name, byte[] value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    public void setBytes(String name, byte[] value, int offset, int length) throws JMSException {
        byte[] exact = new byte[length];
        System.arraycopy(value,offset,exact,0,length);
        setBytes(name,exact);
    }

    public void setObject(String name, Object value) throws JMSException {
        preWrite();
        data.put(name,value);
    }

    private void preWrite() {
        // TODO: read-only mode check
    }

    private static final long serialVersionUID = 1L;
}
