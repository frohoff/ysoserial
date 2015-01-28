package ysoserial.payloads;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

import java.io.FilePermission;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSecurityManager;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import ysoserial.ExecSerializable;
import ysoserial.MockPayload;
import ysoserial.MockSecurityManager;
import ysoserial.Throwables;
import ysoserial.payloads.CommonsCollections1;
import ysoserial.payloads.Groovy1;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.Spring1;
import ysoserial.payloads.util.Serializables;

/*
 * tests each of the parameterize Payload classes by using a mock SecurityManager that throws
 * a special exception when an exec() attempt is made for more reliable detection; self-tests
 * the harness for trivial pass and failure cases
 */
@SuppressWarnings({"restriction","unused"})
@RunWith(Theories.class)
public class PayloadsTest {
	private static final String ASSERT_MESSAGE = "should have thrown " + ExecException.class.getSimpleName();

	@SuppressWarnings("serial")
	private static class ExecException extends RuntimeException {}
	private final MockSecurityManager msm = new MockSecurityManager(){
		public void checkExec(final String cmd) {
			super.checkExec(cmd);
			// throw a special exception to ensure we can detect exec() in the test
			throw new ExecException();
		};
	};
	
	@Rule
	public final ProvideSecurityManager psm = new ProvideSecurityManager(msm);
	
	@DataPoints
	public static ObjectPayload[] payloads() {
		return new ObjectPayload[] { new CommonsCollections1(), new Groovy1(), new Spring1() };
	}	

	@Theory
	public void testPayload(final ObjectPayload payload) throws Exception {		
		final Object f = payload.getObject("hostname");
		final byte[] serialized = Serializables.serialize(f);
				
		// special case for using TemplatesImpl gadgets with SecurityManager
		System.setProperty(DESERIALIZE_TRANSLET, "true");
		
		try {
			final Object obj = Serializables.deserialize(serialized);
			Assert.fail(ASSERT_MESSAGE); // should never get here
		} catch (Exception e) {
			// hopefully everything will reliably nest our ExecException
			Assert.assertEquals(Throwables.getInnermostCause(e).getClass(), ExecException.class);
		}		
		
		// confirm sm saw the check for file execution
		Assert.assertTrue(msm.getChecks().contains(new FilePermission("<<ALL FILES>>", "execute")));
	}
	
	// make sure test harness fails properly
	@Test
	public void testHarnessFail() throws Exception {
		try {
			testPayload(new MockPayload(1));
			Assert.fail("should have failed");
		} catch (AssertionError e) {
			Assert.assertEquals(ASSERT_MESSAGE, e.getMessage());
		}
	}
	
	// make sure test harness passes properly	
	@Test
	public void testHarnessPass() throws Exception {
		testPayload(new MockPayload(new ExecSerializable()));
	}
}
