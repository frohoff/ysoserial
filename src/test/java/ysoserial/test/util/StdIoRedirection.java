package ysoserial.test.util;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
  Replace System.out/err early-on with proxies that delegate to streams controlled here to ensure changes reflected
  even when references are saved by various writing/logging classes
 */
public class StdIoRedirection {

    private static final PrintStream realOut = System.out;
    private static final PrintStream realErr = System.err;

    private static PrintStream delegateOut = System.out;
    private static PrintStream delegateErr = System.err;

    private static PrintStream proxyOut;
    private static PrintStream proxyErr;

    static {
        try {
            proxyOut = (PrintStream) new ProxyFactory() {{
                this.setSuperclass(PrintStream.class);
            }}.create(new Class[]{OutputStream.class}, new Object[]{delegateOut}, new MethodHandler() {
                @Override
                public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                    return thisMethod.invoke(delegateOut, args);
                }
            });

            proxyErr = (PrintStream) new ProxyFactory() {{
                this.setSuperclass(PrintStream.class);
            }}.create(new Class[]{OutputStream.class}, new Object[]{delegateErr}, new MethodHandler() {
                @Override
                public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
                    return thisMethod.invoke(delegateErr, args);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        System.setOut(proxyOut);
        System.setErr(proxyErr);
    }

    public static void restoreStreams() {
        setStreams(realOut, realErr);
    }

    public static void setStreams(PrintStream out, PrintStream err) {
        delegateOut = out;
        delegateErr = err;
    }

    public static void setStreams(OutputStream out, OutputStream err) {
        setStreams(new PrintStream(out), new PrintStream(err));
    }
}
