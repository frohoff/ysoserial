package ysoserial.secmgr;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class DelegateSecurityManager extends SecurityManager {
	private SecurityManager securityManager;

	public SecurityManager getSecurityManager() {
		return securityManager;
	}

	public void setSecurityManager(SecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	@Override
	public boolean getInCheck() {
		return getSecurityManager().getInCheck();
	}

	@Override
	public Object getSecurityContext() {
		return getSecurityManager().getSecurityContext();
	}

	@Override
	public void checkPermission(Permission perm) {
		getSecurityManager().checkPermission(perm);
	}

	@Override
	public void checkPermission(Permission perm, Object context) {
		getSecurityManager().checkPermission(perm, context);
	}

	@Override
	public void checkCreateClassLoader() {
		getSecurityManager().checkCreateClassLoader();
	}

	@Override
	public void checkAccess(Thread t) {
		getSecurityManager().checkAccess(t);
	}

	@Override
	public void checkAccess(ThreadGroup g) {

		getSecurityManager().checkAccess(g);
	}

	@Override
	public void checkExit(int status) {

		getSecurityManager().checkExit(status);
	}

	@Override
	public void checkExec(String cmd) {

		getSecurityManager().checkExec(cmd);
	}

	@Override
	public void checkLink(String lib) {

		getSecurityManager().checkLink(lib);
	}

	@Override
	public void checkRead(FileDescriptor fd) {

		getSecurityManager().checkRead(fd);
	}

	@Override
	public void checkRead(String file) {

		getSecurityManager().checkRead(file);
	}

	@Override
	public void checkRead(String file, Object context) {

		getSecurityManager().checkRead(file, context);
	}

	@Override
	public void checkWrite(FileDescriptor fd) {

		getSecurityManager().checkWrite(fd);
	}

	@Override
	public void checkWrite(String file) {

		getSecurityManager().checkWrite(file);
	}

	@Override
	public void checkDelete(String file) {

		getSecurityManager().checkDelete(file);
	}

	@Override
	public void checkConnect(String host, int port) {

		getSecurityManager().checkConnect(host, port);
	}

	@Override
	public void checkConnect(String host, int port, Object context) {

		getSecurityManager().checkConnect(host, port, context);
	}

	@Override
	public void checkListen(int port) {

		getSecurityManager().checkListen(port);
	}

	@Override
	public void checkAccept(String host, int port) {

		getSecurityManager().checkAccept(host, port);
	}

	@Override
	public void checkMulticast(InetAddress maddr) {

		getSecurityManager().checkMulticast(maddr);
	}

	@Override
	public void checkMulticast(InetAddress maddr, byte ttl) {

		getSecurityManager().checkMulticast(maddr, ttl);
	}

	@Override
	public void checkPropertiesAccess() {

		getSecurityManager().checkPropertiesAccess();
	}

	@Override
	public void checkPropertyAccess(String key) {

		getSecurityManager().checkPropertyAccess(key);
	}

	@Override
	public boolean checkTopLevelWindow(Object window) {

		return getSecurityManager().checkTopLevelWindow(window);
	}

	@Override
	public void checkPrintJobAccess() {

		getSecurityManager().checkPrintJobAccess();
	}

	@Override
	public void checkSystemClipboardAccess() {

		getSecurityManager().checkSystemClipboardAccess();
	}

	@Override
	public void checkAwtEventQueueAccess() {

		getSecurityManager().checkAwtEventQueueAccess();
	}

	@Override
	public void checkPackageAccess(String pkg) {

		getSecurityManager().checkPackageAccess(pkg);
	}

	@Override
	public void checkPackageDefinition(String pkg) {

		getSecurityManager().checkPackageDefinition(pkg);
	}

	@Override
	public void checkSetFactory() {

		getSecurityManager().checkSetFactory();
	}

	@Override
	public void checkMemberAccess(Class<?> clazz, int which) {

		getSecurityManager().checkMemberAccess(clazz, which);
	}

	@Override
	public void checkSecurityAccess(String target) {

		getSecurityManager().checkSecurityAccess(target);
	}

	@Override
	public ThreadGroup getThreadGroup() {

		return getSecurityManager().getThreadGroup();
	}
}