package ysoserial.test.util;

import java.io.File;

public class Files {
    public static void waitForFile(File file, int timeoutMs) throws InterruptedException {
        long timeout = System.currentTimeMillis() + timeoutMs;
        while (! file.exists() && System.currentTimeMillis() < timeout) {
            Thread.sleep(10);
        }
    }
}
