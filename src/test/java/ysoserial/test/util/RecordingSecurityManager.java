package ysoserial.test.util;

import java.security.Permission;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecordingSecurityManager extends SecurityManager {
    private final List<Permission> checks = new LinkedList<Permission>();

    public List<Permission> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    @Override
    public void checkPermission(Permission perm) {
//        System.err.println("check " + perm);
        checks.add(perm);
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }
}
