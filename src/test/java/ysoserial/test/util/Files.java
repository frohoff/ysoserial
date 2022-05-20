package ysoserial.test.util;

import ysoserial.Strings;

import java.io.File;
import java.io.PrintWriter;

public class Files {
    public static void waitForFile(File file, int timeoutMs) throws InterruptedException {
        long timeout = System.currentTimeMillis() + timeoutMs;
        while (! file.exists() && System.currentTimeMillis() < timeout) {
            Thread.sleep(10);
        }
    }

    public static void writeFile(File file, String content) {
        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getTestFile() {
        return new File(OS.getTmpDir(), "ysoserial-test-" + Strings.randUUID());
    }
}
