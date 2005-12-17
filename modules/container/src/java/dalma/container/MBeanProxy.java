package dalma.container;

import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy MBean that avoid strong reference to the real MBean object.
 *
 * @author Kohsuke Kawaguchi
 */
final class MBeanProxy implements InvocationHandler, MBeanRegistration {

    /**
     * Creates a proxy MBean and registers it to the server,
     * overriding the existing mbean if necessary.
     */
    public static <T> void register( MBeanServer server, ObjectName name, Class<T> mbeanInterface, T object ) throws JMException {
        Object proxy = mbeanInterface.cast(
            Proxy.newProxyInstance(mbeanInterface.getClassLoader(),
            new Class[]{mbeanInterface,MBeanRegistration.class},
            new MBeanProxy(object) ));

        if(server.isRegistered(name)) {
            try {
                server.unregisterMBean(name);
            } catch (JMException e) {
                // if we fail to unregister, try to register ours anyway.
                // maybe a GC kicked in in-between.
            }
        }

        // since the proxy class has random names like '$Proxy1',
        // we need to use StandardMBean to designate a management interface
        server.registerMBean(new StandardMBean(proxy,mbeanInterface),name);

    }

    /**
     * The real MBean object.
     */
    private final ReferenceImpl real;
    private MBeanServer server;
    private ObjectName name;

    private MBeanProxy(Object realObject) {
        this.real = new ReferenceImpl(realObject);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object o = real.get();

        if(method.getDeclaringClass()==MBeanRegistration.class) {
            o = this;
        }

        if(o==null) {
            unregister();
            throw new IllegalStateException(name+" no longer exists");
        }

        try {
            return method.invoke(o,args);
        } catch (InvocationTargetException e) {
            if(e.getCause()!=null)
                throw e.getCause();
            else
                throw e;
        }
    }

    private void unregister() {
        try {
            server.unregisterMBean(name);
        } catch (JMException e) {
            throw new Error(e); // is this even possible?
        }
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        this.name = name;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        // noop
    }

    public void preDeregister() throws Exception {
        // noop
    }

    public void postDeregister() {
        server = null;
        name = null;
    }

    private class ReferenceImpl extends WeakReference<Object> {
        public ReferenceImpl(Object referent) {
            super(referent,queue);
        }

        public MBeanProxy getProxy() {
            return MBeanProxy.this;
        }
    }

    private static final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();

    static {
        Runnable r = new Runnable() {
            public void run() {
                while (true) {
                    ReferenceImpl ref;
                    try {
                        ref = (ReferenceImpl) (WeakReference<Object>)queue.remove();
                    } catch (InterruptedException e) {
                        return;
                    }
                    ref.getProxy().unregister();
                }
            }
        };
        Thread t = new Thread(r, "Dalma JMX bean clenaer");
        t.setDaemon(true);
        t.start();
    }
}
