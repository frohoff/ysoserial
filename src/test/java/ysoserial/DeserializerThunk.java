package ysoserial;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;

/*
 * deserializes specified bytes; for use from isolated classloader
 */
public class DeserializerThunk implements Callable<Object> {
	private final byte[] bytes;		
	public DeserializerThunk(byte[] bytes) { this.bytes = bytes; }
	public Object call() throws Exception {
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return ois.readObject();
	}
}