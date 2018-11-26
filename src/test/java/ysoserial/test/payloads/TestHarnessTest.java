package ysoserial.test.payloads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import ysoserial.payloads.ObjectPayload;

public class TestHarnessTest {
	// make sure test harness fails properly
	@Test
	public void testHarnessExecFail() throws Exception {
		try {
			PayloadsTest.testPayload(NoopMockPayload.class, new Class[0]);
			Assert.fail("should have failed");
		} catch (AssertionError e) {
			Assert.assertThat(e.getMessage(), CoreMatchers.containsString("test file should exist"));

		}
	}

	// make sure test harness fails properly
	@Test
	public void testHarnessClassLoaderFail() throws Exception {
		try {
			PayloadsTest.testPayload(ExecMockPayload.class, new Class[0]);
			Assert.fail("should have failed");
		} catch (AssertionError e) {
			//Assert.assertThat(e.getMessage(), CoreMatchers.containsString("ClassNotFoundException"));
		}
	}

	// make sure test harness passes properly with trivial execution gadget
	@Test
	public void testHarnessExecPass() throws Exception {
		PayloadsTest.testPayload(ExecMockPayload.class, new Class[] { ExecMockSerializable.class });
	}

	public static class ExecMockPayload implements ObjectPayload<ExecMockSerializable> {
		public ExecMockSerializable getObject(String command) throws Exception {
			return new ExecMockSerializable(command);
		}
	}

	public static class NoopMockPayload implements ObjectPayload<Integer> {
		public Integer getObject(String command) throws Exception {
			return 1;
		}
	}

	@SuppressWarnings("serial")
	public static class ExecMockSerializable implements Serializable {
		private final String cmd;
		public ExecMockSerializable(String cmd) { this.cmd = cmd; }

		private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
		    ois.defaultReadObject();
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
