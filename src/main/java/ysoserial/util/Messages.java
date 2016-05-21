package ysoserial.util;

import java.text.MessageFormat;

public class Messages {

	public static void println( String message, Object... params ) {
		System.err.println( "[*] " + MessageFormat.format( message, params ) );
	}

}
