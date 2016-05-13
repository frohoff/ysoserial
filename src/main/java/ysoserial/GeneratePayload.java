package ysoserial;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ysoserial.Serializer.Format;
import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.Utils;
import ysoserial.payloads.annotation.Dependencies;

@SuppressWarnings("rawtypes")
public class GeneratePayload {

	public static class ToStringComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) { return o1.toString().compareTo(o2.toString()); }
	}
	private static final int INTERNAL_ERROR_CODE = 70;
	
	private static final int USAGE_CODE = 64;

	public static void main(final String[] args) {
		if (args.length < 1) {
			printUsage();
			System.exit(USAGE_CODE);
		}
		Format format = Format.Raw;
		
		int genArgs = 0;
		// Find generator args (as opposed to payload args)
		for( int i = 0; i < args.length; i++ ) {
			if ( !args[i].startsWith( "-" ) ) {
				break;
			}
			
			if ( args[i].equals( "-raw" ) ) {
				format = Format.Raw;
			} else if ( args[i].equals( "-hex" ) ) { 
				format = Format.Hex;
			} else if ( args[i].equals( "-base64" ) ) {
				format = Format.Base64;
			}
			
			genArgs++;
		}

		final String payloadType = args[genArgs++];

		final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
		if (payloadClass == null) {
			System.err.println("Invalid payload type '" + payloadType + "'");
			printUsage();
			System.exit(USAGE_CODE);
			return; // make null analysis happy
		}
		
		
		String[] newArgs = new String[ args.length - genArgs ];
		System.arraycopy( args, genArgs, newArgs, 0, newArgs.length );

		try {
			try {
				final ObjectPayload payload = payloadClass.newInstance();
				Utils.wire( payload, newArgs );
				
				final Object object = payload.getObject();
				PrintStream out = System.out;
				Serializer.serialize(object, out, format);
				Utils.releasePayload(payload, object);
			} catch( IllegalArgumentException e ) {
				System.err.println( "Invalid arguments for payload type '" + payloadType + "'" );
				printHelp( payloadClass );
			}
		} catch (Throwable e) {
			System.err.println("Error while generating or serializing payload");
			e.printStackTrace();
			System.exit(INTERNAL_ERROR_CODE);
		}
		System.exit(0);
	}

	private static void printHelp(Class<? extends ObjectPayload> payloadClass) throws Exception {
		System.err.println( "Y SO SERIAL?" );
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [format] " + payloadClass.getSimpleName() + " [params...]");
		System.err.println( "Available formats: -raw, -hex, -base64" );
		System.err.println( "Parameters for this payload type: " );
		
		Set<String> params = new TreeSet<String>();
		
		Class<?> cls = payloadClass;
		while( cls != null ) {
			for( Field f : cls.getDeclaredFields() ) {
				if ( f.getAnnotation( Bind.class ) != null ) {
					Bind binding = f.getAnnotation( Bind.class );
					params.add( f.getName() + " [" + type(f) + "]: " + binding.helpText() );
				}
			}
			
			cls = cls.getSuperclass();
		}
		
		for( String s : params ) {
			System.err.println( " * " + s );
		}
	}

	private static void printUsage() {
		System.err.println("Y SO SERIAL?");
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [format] payload_type [params...]");
		System.err.println( "Available formats: -raw, -hex, -base64" );
		System.err.println("\tAvailable payload types:");
		final List<Class<? extends ObjectPayload>> payloadClasses =
			new ArrayList<Class<? extends ObjectPayload>>(Utils.getPayloadClasses());
		Collections.sort(payloadClasses, new ToStringComparator()); // alphabetize
		for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
			System.err.println("\t\t" + payloadClass.getSimpleName() + " " + Arrays.asList(Dependencies.Utils.getDependencies(payloadClass)));
		}
	}

	private static String type(Field f) {
		Class<?> type = f.getType();
		if ( type.equals( String.class ) ) {
			return "string";
		} else if ( type.equals( URL.class ) ) {
			return "url";
		} else if ( type.equals( Integer.TYPE ) ) {
			return "number";
		} else if ( type.equals( Long.TYPE ) ) {
			return "number";
		} else if ( type.equals( Boolean.TYPE ) ) {
			return "boolean";
		} else if ( type.equals( File.class ) ) { 
			return "filename";
		} else if ( type.equals( FileInputStream.class ) ) {
			return "filename";
		} else if ( type.equals( ObjectPayload.class ) || ObjectPayload.class.isAssignableFrom( type ) ) {
			return "payload name";
		}
		
		return "unknown";
	}

}
