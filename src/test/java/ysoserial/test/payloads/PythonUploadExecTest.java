package ysoserial.test.payloads;

import org.junit.Assert;
import ysoserial.Strings;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Files;
import ysoserial.test.util.OS;

import java.io.File;
import java.util.concurrent.Callable;

public class PythonUploadExecTest implements CustomTest {
    private final File testFile = new File(OS.getTmpDir(), "ysoserial-test-" + Strings.randUUID());
    private final File srcPyFile = new File(OS.getTmpDir(), "ysoserial-test-src-" + Strings.randUUID() + ".py");
    private final File dstPyFile = new File(OS.getTmpDir(), "ysoserial-test-dst-" + Strings.randUUID() + ".py");
    private final String testCode = "open('" + testFile + "','w').close()";

    {
        Files.writeFile(srcPyFile, testCode);
    }

    @Override
    public void run(Callable<Object> payload) throws Exception {
        Assert.assertTrue("test src file should exist", srcPyFile.exists());

        Assert.assertFalse("test file should not exist", testFile.exists());
        try {
            payload.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Files.waitForFile(testFile, 5000);
        Assert.assertTrue("test dst file should exist", dstPyFile.exists());
        Assert.assertTrue("test file should exist", testFile.exists());
        testFile.deleteOnExit();
        srcPyFile.deleteOnExit();
        dstPyFile.deleteOnExit();
    }

    @Override
    public String getPayloadArgs() {
        return srcPyFile + ";" + dstPyFile;
    }

}
