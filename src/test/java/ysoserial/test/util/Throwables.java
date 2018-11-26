package ysoserial.test.util;

public class Throwables {
	public static Throwable getInnermostCause(final Throwable t) {
		final Throwable cause = t.getCause();
		return cause == null || cause == t ? t : getInnermostCause(cause);
	}
}
