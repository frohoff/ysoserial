package ysoserial;

import java.security.Permission;
import java.util.LinkedList;
import java.util.List;

public class MockSecurityManager extends SecurityManager {
	private final List<Permission> checks = new LinkedList<Permission>();

	public List<Permission> getChecks() {
		return checks;
	}

	@Override
	public void checkPermission(final Permission perm) {
		checks.add(perm);
	}
	
	@Override
	public void checkPermission(final Permission perm, final Object context) {
		checks.add(perm);
	}
		
}