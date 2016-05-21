package ysoserial.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Arguments {

	private static Stack<Map<String, String>> args = new Stack<Map<String, String>>();
	
	public static Map<String, String> peek() { 
		if ( args.size() == 0 ) {
			return new HashMap<String, String>();
		} else {
			return args.peek();
		}
	}
	
	public static Map<String, String> pop() { 
		return args.pop();
	}
	
	public static void push( Map<String, String> args ) { 
		Arguments.args.push( args );
	}
	
	public static void push( String[] args ) { 
		Arguments.args.push( parseArguments( args ) );
	}

	public static Map<String, String> parseArguments(String[] arguments) {
		Map<String, String> params = new HashMap<String, String>();
		if ( arguments.length == 1 ) {
			params.put( "command", arguments[0] );
		} else {
			for( int i = 0; i < arguments.length; i += 2 ) {
				String argName = arguments[i];
				
				if ( argName.startsWith( "-" ) ) {
					argName = argName.substring( 1 );
				}
				
				if ( arguments.length == (i + 1) ) {
					// No more left, this has to be boolean
					params.put( argName,  "true" );
					continue;
				}
				
				String argVal = arguments[i + 1];
				
				if ( argVal.startsWith( "-" ) ) {
					// This is a boolean "presence" argument
					i--;
					params.put( argName, "true" );
				} else {
					// This is a regular argument
					params.put( argName, argVal );
				}
			}
		}
		return params;
	}
	
}
