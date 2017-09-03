package ysoserial.payloads;


import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSecurityManager;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ysoserial.CustomDeserializer;
import ysoserial.CustomPayloadArgs;
import ysoserial.CustomTest;
import ysoserial.Deserializer;
import ysoserial.Serializer;
import ysoserial.Throwables;
import ysoserial.WrappedTest;
import ysoserial.payloads.TestHarnessTest.ExecMockPayload;
import ysoserial.payloads.TestHarnessTest.NoopMockPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.ClassFiles;
import ysoserial.secmgr.ExecCheckingSecurityManager;
import ysoserial.secmgr.ExecCheckingSecurityManager.ExecException;


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

    private static final String ASSERT_MESSAGE = "should have thrown " + ExecException.class.getSimpleName();


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


    @Test
    public void testPayload () throws Exception {
        testPayload(payloadClass, new Class[0]);
    }


    public static void testPayload ( final Class<? extends ObjectPayload<?>> payloadClass, final Class<?>[] addlClassesForClassLoader )
            throws Exception {
        String command = "hostname";
        String[] deps = buildDeps(payloadClass);

        PayloadTest t = payloadClass.getAnnotation(PayloadTest.class);

        if ( t != null ) {
            if ( !t.skip().isEmpty() ) {
                Assume.assumeTrue(t.skip(), false);
            }

            if ( !t.precondition().isEmpty() ) {
                Assume.assumeTrue("Precondition", checkPrecondition(payloadClass, t.precondition()));
            }
        }

        String payloadCommand = command;
        Class<?> customDeserializer = null;
        Object wrapper = null;
        if ( t != null && !t.harness().isEmpty() ) {
            Class<?> wrapperClass = Class.forName(t.harness());
            try {
                wrapper = wrapperClass.getConstructor(String.class).newInstance(command);
            } catch ( NoSuchMethodException e ) {
                wrapper = wrapperClass.newInstance();
            }

            if ( wrapper instanceof CustomPayloadArgs ) {
                payloadCommand = ( (CustomPayloadArgs) wrapper ).getPayloadArgs();
            }

            if ( wrapper instanceof CustomDeserializer ) {
                customDeserializer = ((CustomDeserializer)wrapper).getCustomDeserializer();
            }
        }

        ExecCheckingSecurityManager sm = new ExecCheckingSecurityManager();
        final byte[] serialized = sm.wrap(makeSerializeCallable(payloadClass, payloadCommand));
        Callable<Object> callable = makeDeserializeCallable(t, addlClassesForClassLoader, deps, serialized, customDeserializer);
        if ( wrapper instanceof WrappedTest ) {
            callable = ( (WrappedTest) wrapper ).createCallable(callable);
        }

        if ( wrapper instanceof CustomTest ) {
            ( (CustomTest) wrapper ).run(callable);
            return;
        }
        try {

            Object deserialized = sm.wrap(callable);
            Assert.fail(ASSERT_MESSAGE); // should never get here
        }
        catch ( Throwable e ) {
            // hopefully everything will reliably nest our ExecException
            Throwable innerEx = Throwables.getInnermostCause(e);
            if ( ! ( innerEx instanceof ExecException ) ) {
                innerEx.printStackTrace();
            }
            Assert.assertEquals(ExecException.class, innerEx.getClass());
            Assert.assertEquals(command, ( (ExecException) innerEx ).getCmd());
        }

        Assert.assertEquals(Arrays.asList(command), sm.getCmds());
    }



    private static Callable<byte[]> makeSerializeCallable ( final Class<? extends ObjectPayload<?>> payloadClass, final String command ) {
        return new Callable<byte[]>() {

            public byte[] call () throws Exception {
                ObjectPayload<?> payload = payloadClass.newInstance();
                final Object f = payload.getObject(command);
                byte[] serialized =  Serializer.serialize(f);
                ObjectPayload.Utils.releasePayload(payload, f);
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


    static Object deserializeWithDependencies ( byte[] serialized, final String[] dependencies, final Class<?>[] classDependencies, final Class<?> customDeserializer )
            throws Exception {
        File[] jars = dependencies.length > 0 ? Maven.resolver().resolve(dependencies).withoutTransitivity().asFile() : new File[0];
        URL[] urls = new URL[jars.length];
        for ( int i = 0; i < jars.length; i++ ) {
            urls[ i ] = jars[ i ].toURI().toURL();
        }

        URLClassLoader isolatedClassLoader = new URLClassLoader(urls, null) {

            {
                for ( Class<?> clazz : classDependencies ) {
                    byte[] classAsBytes = ClassFiles.classAsBytes(clazz);
                    defineClass(clazz.getName(), classAsBytes, 0, classAsBytes.length);
                }
                byte[] deserializerClassBytes = ClassFiles.classAsBytes(Deserializer.class);
                defineClass(Deserializer.class.getName(), deserializerClassBytes, 0, deserializerClassBytes.length);

                if ( customDeserializer != null ) {

                    try {
                        Method method = customDeserializer.getMethod("getExtraDependencies");
                        for ( Class extra : (Class[])method.invoke(null)) {
                            deserializerClassBytes = ClassFiles.classAsBytes(extra);
                            defineClass(extra.getName(), deserializerClassBytes, 0, deserializerClassBytes.length);
                        }
                    } catch ( NoSuchMethodException e ) { }

                    deserializerClassBytes = ClassFiles.classAsBytes(customDeserializer);
                    defineClass(customDeserializer.getName(), deserializerClassBytes, 0, deserializerClassBytes.length);
                }

            }
        };

        Class<?> deserializerClass = isolatedClassLoader.loadClass(customDeserializer != null ? customDeserializer.getName() : Deserializer.class.getName());
        Callable<Object> deserializer = (Callable<Object>) deserializerClass.getConstructors()[ 0 ].newInstance(serialized);
        final Object obj = deserializer.call();
        return obj;
    }
}
