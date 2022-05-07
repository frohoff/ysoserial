package ysoserial.test.payloads;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import ysoserial.Strings;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.Callable;

public class SimpleFileWriteTest implements CustomTest {
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
        String testFileContent = new BufferedReader(new FileReader(testFile)).readLine();
        Assert.assertEquals(testContent.trim(), testFileContent.trim());
        testFile.deleteOnExit();
    }

    @Override
    public String getPayloadArgs() {
        return testFile.toString() + ";" + Base64.encodeBase64String(testContent.getBytes());
    }

}
