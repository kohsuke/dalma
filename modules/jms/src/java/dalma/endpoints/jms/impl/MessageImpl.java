package dalma.endpoints.jms.impl;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

/**
 *
 * @ThirdParty this class contains code released under ASL.
 * @author Kohsuke Kawaguchi
 */
abstract class MessageImpl<T extends Message> implements Message, Serializable {
    String jmsMessageId;
    long jmsTimestamp;
    private String jmsCorrelationID;
    private Destination jmsReplyTo;
    private Destination jmsDestination;
    private int jmsDeliveryMode;
    private boolean jmsRedelivered;
    private String jmsType;
    private long jmsExpiration;
    private int jmsPriority;
    private Map<String,Object> properties;

    /**
     * If this wrapper is still connected to the original message,
     * this field points to that. Otherwise null.
     */
    private transient Message original;

    protected MessageImpl() {}

    protected MessageImpl wrap(T s) throws JMSException {
        this.original = s;
        jmsMessageId        = s.getJMSMessageID();
        jmsTimestamp        = s.getJMSTimestamp();
        jmsCorrelationID    = s.getJMSCorrelationID();
        jmsReplyTo          = s.getJMSReplyTo();    // TODO
        jmsDestination      = s.getJMSDestination();    // TODO
        jmsDeliveryMode     = s.getJMSDeliveryMode();
        jmsRedelivered      = s.getJMSRedelivered();
        jmsType             = s.getJMSType();
        jmsExpiration       = s.getJMSExpiration();
        jmsPriority         = s.getJMSPriority();
        properties          = new HashMap<String, Object>();
        Enumeration e = s.getPropertyNames();
        while(e.hasMoreElements()) {
            String key = (String) e.nextElement();
            properties.put(key,s.getObjectProperty(key));
        }
        clearBody();
        return this;
    }

    protected void writeTo(T d) throws JMSException {
        d.setJMSMessageID(jmsMessageId);
        d.setJMSTimestamp(jmsTimestamp);
        d.setJMSCorrelationID(jmsCorrelationID);
        d.setJMSReplyTo(jmsReplyTo);
        d.setJMSDestination(jmsDestination);
        d.setJMSDeliveryMode(jmsDeliveryMode);
        d.setJMSRedelivered(jmsRedelivered);
        d.setJMSType(jmsType);
        d.setJMSExpiration(jmsExpiration);
        d.setJMSPriority(jmsPriority);
        d.clearProperties();
        Enumeration e = getPropertyNames();
        while(e.hasMoreElements()) {
            String key = (String) e.nextElement();
            d.setObjectProperty(key,getObjectProperty(key));
        }
        d.clearBody();
    }

    public String getJMSMessageID() throws JMSException {
        return jmsMessageId;
    }

    public void setJMSMessageID(String id) throws JMSException {
        this.jmsMessageId = id;
    }

    public long getJMSTimestamp() throws JMSException {
        return jmsTimestamp;
    }

    public void setJMSTimestamp(long timestamp) throws JMSException {
        this.jmsTimestamp = timestamp;
    }

    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public void setJMSCorrelationID(String correlationID) throws JMSException {
        this.jmsCorrelationID = correlationID;
    }

    public String getJMSCorrelationID() throws JMSException {
        return jmsCorrelationID;
    }

    public Destination getJMSReplyTo() throws JMSException {
        return jmsReplyTo;
    }

    public void setJMSReplyTo(Destination replyTo) throws JMSException {
        this.jmsReplyTo = replyTo;
    }

    public Destination getJMSDestination() throws JMSException {
        return jmsDestination;
    }

    public void setJMSDestination(Destination destination) throws JMSException {
        this.jmsDestination = destination;
    }

    public int getJMSDeliveryMode() throws JMSException {
        return jmsDeliveryMode;
    }

    public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
        this.jmsDeliveryMode = deliveryMode;
    }

    public boolean getJMSRedelivered() throws JMSException {
        return jmsRedelivered;
    }

    public void setJMSRedelivered(boolean redelivered) throws JMSException {
        this.jmsRedelivered = redelivered;
    }

    public String getJMSType() throws JMSException {
        return jmsType;
    }

    public void setJMSType(String type) throws JMSException {
        this.jmsType = type;
    }

    public long getJMSExpiration() throws JMSException {
        return jmsExpiration;
    }

    public void setJMSExpiration(long expiration) throws JMSException {
        this.jmsExpiration = expiration;
    }

    public int getJMSPriority() throws JMSException {
        return jmsPriority;
    }

    public void setJMSPriority(int priority) throws JMSException {
        this.jmsPriority = priority;
    }

    public void clearProperties() throws JMSException {
        properties.clear();
    }

    public boolean propertyExists(String name) throws JMSException {
        return properties.containsKey(name);
    }

    public boolean getBooleanProperty(String name) throws JMSException {
        return vanillaToBoolean(this.properties, name);
    }

    public byte getByteProperty(String name) throws JMSException {
        return vanillaToByte(this.properties, name);
    }

    public short getShortProperty(String name) throws JMSException {
        return vanillaToShort(this.properties, name);
    }

    public int getIntProperty(String name) throws JMSException {
        return vanillaToInt(this.properties, name);
    }

    public long getLongProperty(String name) throws JMSException {
        return vanillaToLong(this.properties, name);
    }

    public float getFloatProperty(String name) throws JMSException {
        return vanillaToFloat(this.properties, name);
    }

    public double getDoubleProperty(String name) throws JMSException {
        return vanillaToDouble(this.properties, name);
    }

    public String getStringProperty(String name) throws JMSException {
        return vanillaToString(this.properties, name);
    }

    public Object getObjectProperty(String name) {
        return this.properties != null ? this.properties.get(name) : null;
    }

    public Enumeration getPropertyNames() {
        if (this.properties == null) {
            this.properties = new HashMap<String,Object>();
        }
        return Collections.enumeration(this.properties.keySet());
    }

    public void setBooleanProperty(String name, boolean value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, (value) ? Boolean.TRUE : Boolean.FALSE);
    }

    public void setByteProperty(String name, byte value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);
    }

    public void setShortProperty(String name, short value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);
    }

    public void setIntProperty(String name, int value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);
    }

    public void setLongProperty(String name, long value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);
    }

    public void setFloatProperty(String name, float value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);

    }

    public void setDoubleProperty(String name, double value) throws JMSException {
        prepareProperty(name);
        this.properties.put(name, value);
    }

    public void setStringProperty(String name, String value) throws JMSException {
        prepareProperty(name);
        if (value == null) {
            this.properties.remove(name);
        }
        else {
            this.properties.put(name, value);
        }
    }

    boolean vanillaToBoolean(Map table, String name) throws JMSException {
        boolean result = false;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Boolean) {
                result = (Boolean) value;
            }
            else if (value instanceof String) {
                // will throw a runtime exception if cannot convert
                result = Boolean.valueOf((String) value);
            }
            else {
                throw new MessageFormatException(name + " not a Boolean type");
            }
        }
        return result;
    }

    byte vanillaToByte(Map table, String name) throws JMSException {
        byte result = 0;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Byte) {
                result = (Byte) value;
            }
            else if (value instanceof String) {
                result = Byte.valueOf((String) value);
            }
            else {
                throw new MessageFormatException(name + " not a Byte type");
            }
        }
        else {
            //object doesn't exist - so treat as a null ..
            throw new NumberFormatException("Cannot interpret null as a Byte");
        }
        return result;
    }

    short vanillaToShort(Map table, String name) throws JMSException {
        short result = 0;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Short) {
                result = (Short) value;
            }
            else if (value instanceof String) {
                return Short.valueOf((String) value);
            }
            else if (value instanceof Byte) {
                result = (Byte) value;
            }
            else {
                throw new MessageFormatException(name + " not a Short type");
            }
        }
        else {
            throw new NumberFormatException(name + " is null");
        }
        return result;
    }

    int vanillaToInt(Map table, String name) throws JMSException {
        int result = 0;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Integer) {
                result = (Integer) value;
            }
            else if (value instanceof String) {
                result = Integer.valueOf((String) value);
            }
            else if (value instanceof Byte) {
                result = ((Byte) value).intValue();
            }
            else if (value instanceof Short) {
                result = ((Short) value).intValue();
            }
            else {
                throw new MessageFormatException(name + " not an Integer type");
            }
        }
        else {
            throw new NumberFormatException(name + " is null");
        }
        return result;
    }

    long vanillaToLong(Map table, String name) throws JMSException {
        long result = 0;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Long) {
                result = (Long) value;
            }
            else if (value instanceof String) {
                // will throw a runtime exception if cannot convert
                result = Long.valueOf((String) value);
            }
            else if (value instanceof Byte) {
                result = (Byte) value;
            }
            else if (value instanceof Short) {
                result = (Short) value;
            }
            else if (value instanceof Integer) {
                result = (Integer) value;
            }
            else {
                throw new MessageFormatException(name + " not a Long type");
            }
        }
        else {
            throw new NumberFormatException(name + " is null");
        }
        return result;
    }

    float vanillaToFloat(Map table, String name) throws JMSException {
        float result = 0.0f;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Float) {
                result = (Float) value;
            }
            else if (value instanceof String) {
                result = Float.valueOf((String) value);
            }
            else {
                throw new MessageFormatException(name + " not a Float type: " + value.getClass());
            }
        }
        else {
            throw new NullPointerException(name + " is null");
        }
        return result;
    }

    double vanillaToDouble(Map table, String name) throws JMSException {
        double result = 0.0d;
        Object value = getVanillaProperty(table, name);
        if (value != null) {
            if (value instanceof Double) {
                result = (Double) value;
            }
            else if (value instanceof String) {
                result = Double.valueOf((String) value);
            }
            else if (value instanceof Float) {
                result = (Float) value;
            }
            else {
                throw new MessageFormatException(name + " not a Double type");
            }
        }
        else {
            throw new NullPointerException(name + " is null");
        }
        return result;
    }

    Object getVanillaProperty(Map table, String name) {
        Object result = null;
        if (name == null) {
            throw new NullPointerException("name supplied is null");
        }
        result = getReservedProperty(name);
        if (result == null && table != null) {
            result = table.get(name);
        }
        return result;
    }

    Object getReservedProperty(String name){
        Object result = null;
//        if (name != null && name.equals(DELIVERY_COUNT_NAME)){
//            result = new Integer(deliveryCount);
//        }
        return result;
    }


    String vanillaToString(Map table, String name) throws JMSException {
        String result = null;
        if (table != null) {
            Object value = table.get(name);
            if (value != null) {
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    result = value.toString();
                }
                else {
                    throw new MessageFormatException(name + " not a String type");
                }
            }
        }
        return result;
    }

    private void prepareProperty(String name) throws JMSException {
        if (name == null) {
            throw new IllegalArgumentException("Invalid property name: cannot be null");
        }
        if (name.length() == 0) {
            throw new IllegalArgumentException("Invalid property name: cannot be empty");
        }
//        if (this.readOnlyProperties) {
//            throw new MessageNotWriteableException("Properties are read-only");
//        }
        if (this.properties == null) {
            this.properties = new HashMap<String,Object>();
        }
    }

    public void setObjectProperty(String name, Object value) throws JMSException {
        prepareProperty(name);
        if (value == null) {
            this.properties.remove(name);
        }
        else {
            if (value instanceof Number ||
                    value instanceof Character ||
                    value instanceof Boolean ||
                    value instanceof String) {
                this.properties.put(name, value);
            }
            else {
                throw new MessageFormatException("Cannot set property to type: " + value.getClass().getName());
            }
        }
    }

    public void acknowledge() throws JMSException {
        if(original==null)
            throw new UnsupportedOperationException("this message is serialized and therefore too late to aknowledge");
        else
            original.acknowledge();
    }

    private static final long serialVersionUID = 1L;
}
