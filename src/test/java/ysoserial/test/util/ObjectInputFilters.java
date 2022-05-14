package ysoserial.test.util;

import ysoserial.payloads.JRMPListener;
import ysoserial.payloads.util.Reflections;
import ysoserial.test.payloads.JRMPListenerTest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ObjectInputFilters {
    public static Object getAllowFilter() throws Exception {
        final Class<?> filterClass = JRMPListenerTest.loadFirstClass(
            "java.io.ObjectInputFilter", "sun.misc.ObjectInputFilter");
        if (filterClass == null) {
            return null;
        }
        final Class<?> statusClass = Class.forName(filterClass.getName() + "$Status");
        return filterClass != null ? Proxy.newProxyInstance(
            JRMPListener.class.getClass().getClassLoader(),
            new Class[]{ filterClass },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    for (Enum<?> e : (Enum<?>[]) statusClass.getEnumConstants()) {
                        if (e.name() == "ALLOWED") {
                            return e;
                        }
                    }
                    throw new RuntimeException("no matching enum");
                }
            }
        ) : null;
    }

    public static void disableDcgFilter() throws Exception {
        // disable ObjectInputFilter if defined
        Object filter = getAllowFilter();
        if (filter != null) {
            Field f = Reflections.getField(Class.forName("sun.rmi.transport.DGCImpl"), "dgcFilter");
            if (f != null) {
                f.set(null, filter);
            }
        }
    }
}
