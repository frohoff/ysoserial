package ysoserial.test.util;

public enum OS {
    WINDOWS,
    LINUX,
    OSX,
    OTHER;

    private static final OS os = determineOs();

    public static OS get() {
        return os;
    }

    private static OS determineOs() {
        String osName = System.getProperty("os.name", "other").toLowerCase();
        if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("mac os x")) {
            return OSX;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else {
            return OTHER;
        }
    }

    public static String getTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }
}
