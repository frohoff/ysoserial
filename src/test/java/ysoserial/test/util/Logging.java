package ysoserial.test.util;

import ysoserial.test.payloads.PayloadsTest;

import java.io.IOException;
import java.util.logging.LogManager;

public class Logging {
    public static void init() throws IOException {
        LogManager.getLogManager().readConfiguration(PayloadsTest.class.getResourceAsStream("/logging.properties"));
    }
}
