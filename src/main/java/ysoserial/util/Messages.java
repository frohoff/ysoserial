package ysoserial.util;

import java.text.MessageFormat;

public class Messages {
	
	public static void println( String message, Object... params ) {
		if ( !System.getProperty( "ysoserial.suppress.messages", "false" ).equalsIgnoreCase( "true" ) ) { 
			System.err.println( "[*] " + MessageFormat.format( message, params ) );
		}
	}

}
