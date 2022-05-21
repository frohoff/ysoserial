package ysoserial.test.payloads;

import org.junit.Assert;
import ysoserial.payloads.Scala;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;
import ysoserial.test.util.Randomized;

import java.io.File;
import java.util.concurrent.Callable;

public class EmptyFileWriteTest implements CustomTest {
    private final File testFile = new File(OS.getTmpDir(), "ysoserial-test-" + Randomized.randUUID());
    private final String testContent = Randomized.randUUID();

    @Override
    public void run(Callable<Object> payload) throws Exception {
        Assert.assertFalse("test file should not exist", testFile.exists());
        try {
            payload.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Files.waitForFile(testFile, 5000);
        Assert.assertTrue("test file should exist", testFile.exists());
        Assert.assertEquals(0, testFile.length());
        testFile.deleteOnExit();
    }

    @Override
    public String getPayloadArgs() {
        return testFile.toString();
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(Scala.ScalaCreateZeroFile.class, new Class[0]);
    }

}
