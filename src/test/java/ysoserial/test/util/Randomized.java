package ysoserial.test.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.UUID;

public class Randomized {
    public static int randPort() {
        while (true) {
            int port = new Random().nextInt(65535 - 16384) + 16384;
            try {
                ServerSocket s = new ServerSocket(port);
                s.setReuseAddress(true);
                s.close();
                return port;
            } catch (IOException e) {}
        }
    }

    public static String randUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
