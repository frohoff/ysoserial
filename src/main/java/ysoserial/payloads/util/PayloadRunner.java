package ysoserial.payloads.util;

import java.util.concurrent.Callable;

import ysoserial.Deserializer;
import ysoserial.Serializer;
import static ysoserial.Deserializer.deserialize;
import static ysoserial.Serializer.serialize;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.Utils;
import ysoserial.secmgr.ExecCheckingSecurityManager;

/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings("unused")
public class PayloadRunner {
	public static void run(final Class<? extends ObjectPayload<?>> clazz, final String[] args) throws Exception {
		// ensure payload generation doesn't throw an exception
		byte[] serialized = new ExecCheckingSecurityManager().wrap(new Callable<byte[]>(){
			public byte[] call() throws Exception {
				ObjectPayload<?> payload = clazz.newInstance();
				
				Utils.wire( payload, args );
				
                final Object objBefore = payload.getObject();

				System.out.println("serializing payload");
				byte[] ser = Serializer.serialize(objBefore);
				Utils.releasePayload(payload, objBefore);
                return ser;
		}});

		try {
			System.out.println("deserializing payload");
			final Object objAfter = Deserializer.deserialize(serialized);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
