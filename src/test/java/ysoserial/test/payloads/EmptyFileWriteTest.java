package ysoserial.test.payloads;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import ysoserial.Strings;
import ysoserial.payloads.Scala;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.Callable;

public class EmptyFileWriteTest implements CustomTest {
    private final File testFile = new File(OS.getTmpDir(), "ysoserial-test-" + Strings.randUUID());
    private final String testContent = Strings.randUUID();

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
