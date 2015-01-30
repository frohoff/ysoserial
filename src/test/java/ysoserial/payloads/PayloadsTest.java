package ysoserial.payloads;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
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

import ysoserial.DeserializerThunk;
import ysoserial.ExecBlockingSecurityManager;
import ysoserial.ExecBlockingSecurityManager.ExecException;
import ysoserial.ExecSerializable;
import ysoserial.Throwables;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.ClassFiles;
import ysoserial.payloads.util.Serializables;

/*
 * tests each of the parameterize Payload classes by using a mock SecurityManager that throws
 * a special exception when an exec() attempt is made for more reliable detection; self-tests
 * the harness for trivial pass and failure cases

TODO: pull out harness tests so they are only run once
TODO: figure out better way to test exception behavior than comparing messages
 */
@SuppressWarnings({"restriction", "unused", "unchecked"})
@RunWith(Parameterized.class)
public class PayloadsTest {
	private static final String ASSERT_MESSAGE = "should have thrown " + ExecException.class.getSimpleName();
	private static final String DESER_THUNK_CLASS = DeserializerThunk.class.getName();	
	
	@Rule
	public final ProvideSecurityManager psm = new ProvideSecurityManager(new ExecBlockingSecurityManager());
	
	@Parameters(name = "payloadClass: {0}")
	public static Class<? extends ObjectPayload<?>>[] payloads() {
		return new Class[] { CommonsCollections1.class, Groovy1.class , CommonsCollections2.class, Spring1.class };
	}	

	private final Class<? extends ObjectPayload<?>> payloadClass;	
	
	public PayloadsTest(Class<? extends ObjectPayload<?>> payloadClass) {
		this.payloadClass = payloadClass;
	}

	@Test
	public void testPayload() throws Exception {
		testPayload(payloadClass, new Class[0]);
	}
	
	public static void testPayload(final Class<? extends ObjectPayload<?>> payloadClass, Class[] addlClassesForClassLoader) throws Exception {
		String command = "hostname";
		Dependencies depsAnn = payloadClass.getAnnotation(Dependencies.class);
		String[] deps = depsAnn != null ? depsAnn.value() : new String[0];
		ObjectPayload<?> payload = payloadClass.newInstance();
		final Object f = payload.getObject(command);
		final byte[] serialized = Serializables.serialize(f);		
		try {			
			deserializeWithDependencies(serialized, deps, addlClassesForClassLoader);
			Assert.fail(ASSERT_MESSAGE); // should never get here
		} catch (Throwable e) {
			// hopefully everything will reliably nest our ExecException
			Throwable innerEx = Throwables.getInnermostCause(e);
			Assert.assertEquals(ExecException.class, innerEx.getClass());
			Assert.assertEquals(command, ((ExecException) innerEx).getCmd());			
		}		
	}
	
	@SuppressWarnings({ "unchecked" })
	private static void deserializeWithDependencies(byte[] serialized, final String[] dependencies, final Class<?>[] classDependencies) throws Exception {
		// special case for using TemplatesImpl gadgets with a SecurityManager enabled
		System.setProperty(DESERIALIZE_TRANSLET, "true");		
		
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
			byte[] deserializerClassBytes = ClassFiles.classAsBytes(DeserializerThunk.class);
			defineClass(DeserializerThunk.class.getName(), deserializerClassBytes, 0, deserializerClassBytes.length);
											
		}};
		
		Class<?> deserializerClass = isolatedClassLoader.loadClass(DESER_THUNK_CLASS);
		Callable<Object> deserializer = (Callable<Object>) deserializerClass.getConstructors()[0].newInstance(serialized);
		final Object obj = deserializer.call();			
	}
	
	// make sure test harness fails properly
	@Test
	public void testHarnessExecFail() throws Exception {
		try {
			testPayload(NoopMockPayload.class, new Class[0]);
			Assert.fail("should have failed");
		} catch (AssertionError e) {
			Assert.assertThat(e.getMessage(), CoreMatchers.containsString("but was:<class java.lang.AssertionError>"));
			
		}
	}
	
	// make sure test harness fails properly
	@Test
	public void testHarnessClassLoaderFail() throws Exception {
		try {
			testPayload(ExecMockPayload.class, new Class[0]);
			Assert.fail("should have failed");
		} catch (AssertionError e) {
			Assert.assertThat(e.getMessage(), CoreMatchers.containsString("ClassNotFoundException"));
		}
	}	
	
	// make sure test harness passes properly with trivial execution gadget	
	@Test
	public void testHarnessExecPass() throws Exception {		
		testPayload(ExecMockPayload.class, new Class[] { ExecSerializable.class });
	}
	
	public static class ExecMockPayload implements ObjectPayload<ExecSerializable> {
		public ExecSerializable getObject(String command) throws Exception {
			return new ExecSerializable(command);
		}		
	}
	
	public static class NoopMockPayload implements ObjectPayload<Integer> {
		public Integer getObject(String command) throws Exception {
			return 1;
		}		
	}	
}
