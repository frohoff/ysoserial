package ysoserial.payloads;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSecurityManager;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ysoserial.Deserializer;
import ysoserial.Serializer;
import ysoserial.Throwables;
import ysoserial.payloads.TestHarnessTest.ExecMockPayload;
import ysoserial.payloads.TestHarnessTest.NoopMockPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.ClassFiles;
import ysoserial.secmgr.ExecCheckingSecurityManager;
import ysoserial.secmgr.ExecCheckingSecurityManager.ExecException;

/*
 * tests each of the parameterize Payload classes by using a mock SecurityManager that throws
 * a special exception when an exec() attempt is made for more reliable detection; self-tests
 * the harness for trivial pass and failure cases

TODO: figure out better way to test exception behavior than comparing messages
 */
@SuppressWarnings({"restriction", "unused", "unchecked"})
@RunWith(Parameterized.class)
public class PayloadsTest {
	private static final String ASSERT_MESSAGE = "should have thrown " + ExecException.class.getSimpleName();

	@Parameters(name = "payloadClass: {0}")
	public static Class<? extends ObjectPayload<?>>[] payloads() {
		Set<Class<? extends ObjectPayload>> payloadClasses = ObjectPayload.Utils.getPayloadClasses();
		payloadClasses.removeAll(Arrays.asList(ExecMockPayload.class, NoopMockPayload.class));
		return payloadClasses.toArray(new Class[0]);
	}

	private final Class<? extends ObjectPayload<?>> payloadClass;

	public PayloadsTest(Class<? extends ObjectPayload<?>> payloadClass) {
		this.payloadClass = payloadClass;
	}

	@Test
	public void testPayload() throws Exception {
		testPayload(payloadClass, new Class[0]);
	}

	public static void testPayload(final Class<? extends ObjectPayload<?>> payloadClass, final Class[] addlClassesForClassLoader) throws Exception {
		final String command = "hostname";
		final String[] deps = Dependencies.Utils.getDependencies(payloadClass);
		ExecCheckingSecurityManager sm = new ExecCheckingSecurityManager();
		final byte[] serialized = sm.wrap(new Callable<byte[]>(){
			public byte[] call() throws Exception {
				ObjectPayload<?> payload = payloadClass.newInstance();
				final Object f = payload.getObject(command);
				return Serializer.serialize(f);
			}});

		try {
			Object deserialized = sm.wrap(new Callable<Object>(){
				public Object call() throws Exception {
					return deserializeWithDependencies(serialized, deps, addlClassesForClassLoader);
				}
			});

			Assert.fail(ASSERT_MESSAGE); // should never get here
		} catch (Throwable e) {
			// hopefully everything will reliably nest our ExecException
			Throwable innerEx = Throwables.getInnermostCause(e);
			Assert.assertEquals(ExecException.class, innerEx.getClass());
			Assert.assertEquals(command, ((ExecException) innerEx).getCmd());
		}
		Assert.assertEquals(Arrays.asList(command), sm.getCmds());
	}

	@SuppressWarnings({ "unchecked" })
	private static Object deserializeWithDependencies(byte[] serialized, final String[] dependencies, final Class<?>[] classDependencies) throws Exception {
		File[] jars = dependencies.length > 0 ? Maven.resolver().resolve(dependencies).withoutTransitivity().asFile() : new File[0];
		URL[] urls = new URL[jars.length];
		for (int i = 0; i < jars.length; i++) {
			urls[i] = jars[i].toURI().toURL();
		}

		URLClassLoader isolatedClassLoader = new URLClassLoader(urls, null) {{
			for (Class<?> clazz : classDependencies) {
				byte[] classAsBytes = ClassFiles.classAsBytes(clazz);
				defineClass(clazz.getName(), classAsBytes, 0, classAsBytes.length);
			}
			byte[] deserializerClassBytes = ClassFiles.classAsBytes(ysoserial.Deserializer.class);
			defineClass(ysoserial.Deserializer.class.getName(), deserializerClassBytes, 0, deserializerClassBytes.length);

		}};

		Class<?> deserializerClass = isolatedClassLoader.loadClass(ysoserial.Deserializer.class.getName());
		Callable<Object> deserializer = (Callable<Object>) deserializerClass.getConstructors()[0].newInstance(serialized);
		final Object obj = deserializer.call();
		return obj;
	}
}
