package ysoserial;

public class Throwables {
	public static Throwable getInnermostCause(final Throwable t) {
		final Throwable cause = t.getCause();
		return cause == null ? t : getInnermostCause(cause);  
	}		
}