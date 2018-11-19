package ysoserial.payloads.util;


/**
 * @author mbechler
 *
 */
public class JavaVersion {


    public int major;
    public int minor;
    public int update;



    public static JavaVersion getLocalVersion() {
        String property = System.getProperties().getProperty("java.version");
        if ( property == null ) {
            return null;
        }
        JavaVersion v = new JavaVersion();
        String parts[] = property.split("\\.|_|-");
        int start = "1".equals(parts[0]) ? 1 : 0; // skip "1." prefix
        v.major   = Integer.parseInt(parts[start + 0]);
        v.minor   = Integer.parseInt(parts[start + 1]);
        v.update  = Integer.parseInt(parts[start + 2]);
        return v;
    }

    public static boolean isAnnInvHUniversalMethodImpl() {
        JavaVersion v = JavaVersion.getLocalVersion();
        return v != null && (v.major < 8 || (v.major == 8 && v.update <= 71));
    }

    public static boolean isBadAttrValExcReadObj() {
        JavaVersion v = JavaVersion.getLocalVersion();
        return v != null && (v.major > 8 && v.update >= 76);
    }

    public static boolean isAtLeast(int major) {
        JavaVersion v = JavaVersion.getLocalVersion();
        return v != null && v.major >= major;
    }
}

