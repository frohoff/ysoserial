package ysoserial.test.payloads;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ysoserial.*;
import ysoserial.payloads.Atomikos;
import ysoserial.payloads.DynamicDependencies;
import ysoserial.payloads.ObjectPayload;
import ysoserial.test.CustomTest;
import ysoserial.test.CustomDeserializer;
import ysoserial.test.CustomPayloadArgs;
import ysoserial.test.WrappedTest;
import ysoserial.test.payloads.TestHarnessTest.ExecMockPayload;
import ysoserial.test.payloads.TestHarnessTest.NoopMockPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.ClassFiles;
import ysoserial.test.util.Logging;
import ysoserial.test.util.OpenURLClassLoader;
import ysoserial.test.util.PayloadListener;
import ysoserial.test.util.StdIoRedirection;


/*
 * tests each of the parameterize Payload classes by using a mock SecurityManager that throws
 * a special exception when an exec() attempt is made for more reliable detection; self-tests
 * the harness for trivial pass and failure cases

TODO: figure out better way to test exception behavior than comparing messages
 */
@SuppressWarnings ( {
    "rawtypes", "unused", "unchecked"
} )
@RunWith ( Parameterized.class )
public class PayloadsTest {

    @Parameters ( name = "payloadClass: {0}" )
    public static Class<? extends ObjectPayload<?>>[] payloads () {
        Set<Class<? extends ObjectPayload>> payloadClasses = ObjectPayload.Utils.getPayloadClasses();
        payloadClasses.removeAll(Arrays.asList(ExecMockPayload.class, NoopMockPayload.class));
        return payloadClasses.toArray(new Class[0]);
    }

    private final Class<? extends ObjectPayload<?>> payloadClass;


    public PayloadsTest ( Class<? extends ObjectPayload<?>> payloadClass ) {
        this.payloadClass = payloadClass;
    }

    public static void testPayload(Class<? extends ObjectPayload<?>> payloadClass) throws Exception {
        testPayload(payloadClass, new Class[0]);
    }


    @Test
    public void testPayload () throws Exception {
        testPayload(payloadClass, new Class[0]);
    }


    public static void testPayload(final Class<? extends ObjectPayload<?>> payloadClass, Class<?>[] addlClassesForClassLoader)
            throws Exception {
        System.out.println("Testing payload: " + payloadClass.getName());

        String command = "hostname";

        PayloadTest t = payloadClass.getAnnotation(PayloadTest.class);

        int tries = 1;
        if ( t != null ) {
            if (! isForceTests()) {
                if ( !t.skip().isEmpty() ) {
                    Assume.assumeTrue(t.skip(), false);
                }

                if ( !t.precondition().isEmpty() ) {
                    Assume.assumeTrue("Precondition: " + t.precondition(), checkPrecondition(payloadClass, t.precondition()));
                }
            }

            if (! t.flaky().isEmpty()) {
                tries = 5;
            }
        }

        String[] deps = buildDeps(payloadClass);
        String payloadCommand = command;
        Class<?> customDeserializer = null;
        Object testHarness = null;
        if ( t != null && !t.harness().isEmpty() ) {
            Class<?> testHarnessClass = Class.forName(t.harness());
            try {
                testHarness = testHarnessClass.getConstructor(String.class).newInstance(command);
            } catch ( NoSuchMethodException e ) {
                testHarness = testHarnessClass.newInstance();
            }
        } else {
            testHarness = new CommandExecTest(); // default
        }

        if ( testHarness instanceof CustomPayloadArgs) {
            payloadCommand = ( (CustomPayloadArgs) testHarness ).getPayloadArgs();
        }

        if ( testHarness instanceof CustomDeserializer) {
            customDeserializer = ((CustomDeserializer)testHarness).getCustomDeserializer();
        }

        if (testHarness instanceof NeedsAddlClasses) {
            List<Class> classes = new LinkedList<Class>();
            classes.addAll(Arrays.asList(addlClassesForClassLoader));
            classes.addAll(Arrays.asList(((NeedsAddlClasses) testHarness).getAddlClasses()));
            addlClassesForClassLoader = classes.toArray(new Class[classes.size()]);
        }

        // TODO per-thread secmgr to enforce no detonation during deserialization
        final byte[] serialized = makeSerializeCallable(payloadClass, payloadCommand).call();
        Callable<Object> callable = makeDeserializeCallable(t, addlClassesForClassLoader, deps, serialized, customDeserializer);
        if ( testHarness instanceof WrappedTest) {
            callable = ( (WrappedTest) testHarness ).createCallable(callable);
        }


        // if marked as flaky try up to 5 times
        Exception ex = new Exception();
        for (int i = 0; i < tries; i++) {
            try {
                ((CustomTest) testHarness).run(callable);
                ex = null;
                break;
            } catch (Exception e) {
                ex = e;
            }
        }
        if (ex != null) throw ex;
        System.out.println("Successfully tested payload: " + payloadClass.getName());
    }

    private static boolean isForceTests() {
        return System.getProperty("forceTests") != null;
    }

    private static Callable<byte[]> makeSerializeCallable ( final Class<? extends ObjectPayload<?>> payloadClass, final String command ) {
        return new Callable<byte[]>() {

            public byte[] call () throws Exception {
                ObjectPayload<?> payload = payloadClass.newInstance();
                final Object f = payload.getObject(command);
                byte[] serialized =  Serializer.serialize(f);
                ObjectPayload.Utils.postSerializeRelease(payload, f);
                return serialized;
            }
        };
    }

    private static Callable<Object> makeDeserializeCallable ( PayloadTest t, final Class<?>[] addlClassesForClassLoader, final String[] deps,
            final byte[] serialized, final Class<?> customDeserializer ) {
        return new Callable<Object>() {

            public Object call () throws Exception {
                return deserializeWithDependencies(serialized, deps, addlClassesForClassLoader, customDeserializer);
            }
        };
    }

    private static boolean checkPrecondition ( Class<? extends ObjectPayload<?>> pc, String precondition )
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method precondMethod = pc.getMethod(precondition);
        return (Boolean) precondMethod.invoke(null);
    }

    private static String[] buildDeps ( final Class<? extends ObjectPayload<?>> payloadClass ) throws Exception {
        String[] baseDeps;
        if ( DynamicDependencies.class.isAssignableFrom(payloadClass) ) {
            Method method = payloadClass.getMethod("getDependencies");
            baseDeps = (String[]) method.invoke(null);
        }
        else {
            baseDeps = Dependencies.Utils.getDependencies(payloadClass);
        }
        if ( System.getProperty("properXalan") != null ) {
            baseDeps = Arrays.copyOf(baseDeps, baseDeps.length + 1);
            baseDeps[ baseDeps.length - 1 ] = "xalan:xalan:2.7.2";
        }
        return baseDeps;
    }

    static Object deserializeWithDependencies(byte[] serialized, final String[] dependencies, final Class<?>[] classDependencies, final Class<?> customDeserializer)
            throws Exception {
        URL[] urls = getDependencyUrls(dependencies);

        Map<String, byte[]> addlClasses = new HashMap<String, byte[]>();

        for ( Class<?> clazz : classDependencies ) {
            byte[] classAsBytes = ClassFiles.classAsBytes(clazz);
            addlClasses.put(clazz.getName(), classAsBytes);
        }
        addlClasses.put(Deserializer.class.getName(), ClassFiles.classAsBytes(Deserializer.class));

        if (customDeserializer != null) {
            try {
                Method method = customDeserializer.getMethod("getExtraDependencies");
                for (Class extra : (Class[]) method.invoke(null)) {
                    addlClasses.put(extra.getName(), ClassFiles.classAsBytes(extra));
                }
            } catch (NoSuchMethodException e) {}

            addlClasses.put(customDeserializer.getName(), ClassFiles.classAsBytes(customDeserializer));
        }

        OpenURLClassLoader isolatedClassLoader = new OpenURLClassLoader(urls, null);
        for (Map.Entry<String, byte[]> e : addlClasses.entrySet()) {
            isolatedClassLoader.defineNewClass(e.getKey(), e.getValue());
        }

        Class<?> deserializerClass = isolatedClassLoader.loadClass(customDeserializer != null ? customDeserializer.getName() : Deserializer.class.getName());
        Callable<Object> deserializer = (Callable<Object>) deserializerClass.getConstructors()[0].newInstance(serialized);

        // set CCL for Clojure https://groups.google.com/forum/#!topic/clojure/F3ERon6Fye0
        return callWithContextClassLoader(isolatedClassLoader, deserializer);
    }

    private static Object callWithContextClassLoader(ClassLoader classLoader, Callable<Object> callable) throws Exception {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    private static URL[] getDependencyUrls(String[] dependencies) throws MalformedURLException {
        File[] jars = dependencies.length > 0
            ? Maven.configureResolver()
//                .workOffline(JavaVersion.getLocalVersion().major == 6) //  use cached deps for java 1.6
                .withRemoteRepo("central", "https://repo1.maven.org/maven2/", "default")
                .withMavenCentralRepo(false)
                .useLegacyLocalRepo(true)
//                .withRemoteRepo("jenkins", "https://repo.jenkins-ci.org/public/", "default")
                .resolve(dependencies).withoutTransitivity().asFile()
            : new File[0];
        URL[] urls = new URL[jars.length];
        for ( int i = 0; i < jars.length; i++ ) {
            urls[ i ] = jars[ i ].toURI().toURL();
        }
        return urls;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        StdIoRedirection.init();
        Logging.init();

        JUnitCore junit = new JUnitCore();
        PayloadListener listener = new PayloadListener();
        junit.addListener(listener);
        Result result = junit.run(PayloadsTest.class);
        System.exit(isForceTests() ? 0 : (result.wasSuccessful() ? 0 : 1));
    }
}
