package ysoserial;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class Serializer implements Callable<byte[]> {

	public static enum Format { 
		Hex,
		Raw,
		Base64
	}
	
	private final Object object;
	public Serializer(Object object) {
		this.object = object;
	}

	public byte[] call() throws Exception {
		return serialize(object);
	}

	public static byte[] serialize(final Object obj) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialize(obj, out, Format.Raw );
		return out.toByteArray();
	}

	public static void serialize(final Object obj, final OutputStream out, Format format) throws IOException {
		if ( format.equals( Format.Raw ) ) {
			final ObjectOutputStream objOut = new ObjectOutputStream(out);
			objOut.writeObject(obj);
		} else {
			byte[] bytes = serialize( obj );
			DataOutputStream dos = new DataOutputStream( out );
			if ( format.equals( Format.Base64 ) ) {
				dos.write( Base64.encodeBase64(bytes, false) );
			} else if ( format.equals( Format.Hex ) ) {
				dos.writeBytes( Hex.encodeHexString( bytes ) );
			}
		}
	}

}