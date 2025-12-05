package ysoserial;

import java.util.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class Serializer implements Callable<byte[]> {

	private final Object object;
	public Serializer(Object object) {
		this.object = object;
	}

	public byte[] call() throws Exception {
		return serialize(object);
	}

	public static byte[] serialize(final Object obj) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialize(obj, out);
		return out.toByteArray();
	}

	public static void serialize(final Object obj, final OutputStream out) throws IOException {
		final ObjectOutputStream objOut = new ObjectOutputStream(out);
		objOut.writeObject(obj);
	}

	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i + 1 < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public static void serialize(final Object obj, final OutputStream out, final List<String> serials, final List<String> prepended) throws IOException {
		final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		final ObjectOutputStream objOut = new ObjectOutputStream(tmp);
		// prepend
		for (String str : prepended) {
			String [] values = str.split("=");
			String key = values[0];
			String value = values.length > 1 ? values[1] : null;
			try {
				// raw data
				       if (key.equals("write"))        { objOut.write(hexStringToByteArray(value));
				} else if (key.equals("writeBoolean")) { objOut.writeBoolean(new Boolean(value));
				} else if (key.equals("writeByte"))    { objOut.writeByte(Integer.parseInt(value));
				} else if (key.equals("writeBytes"))   { objOut.writeBytes(value);
				} else if (key.equals("writeChar"))    { objOut.writeChar(value.charAt(0));
				} else if (key.equals("writeChars"))   { objOut.writeChars(value);
				} else if (key.equals("writeDouble"))  { objOut.writeDouble(Double.parseDouble(value));
				} else if (key.equals("writeFloat"))   { objOut.writeFloat(Float.parseFloat(value));
				} else if (key.equals("writeInt"))     { objOut.writeInt(Integer.parseInt(value));
				} else if (key.equals("writeLong"))    { objOut.writeLong(Long.parseLong(value));
				} else if (key.equals("writeShort"))   { objOut.writeShort(Integer.parseInt(value));
				} else if (key.equals("writeUTF"))     { objOut.writeUTF(value);
				// other
				} else {
					Class c = null;
					Object o = null;
					c = Class.forName(key);
					if(value == null) {
						o = c.newInstance();
					} else {
						o = c.getConstructor(String.class).newInstance(value);
					}
					objOut.writeObject(o);
				}
			} catch (Exception e) {
				System.err.format("Failed to prepend object %s\n%s\n%s\n", str, e.toString(), e.getMessage());
				e.printStackTrace(System.err);
				System.exit(70);
			}
		}
		objOut.writeObject(obj);
		byte[] bytes = tmp.toByteArray();
		// serialVersionUID
		for (String serial : serials) {
			String [] values = serial.split("=");
			long newUid = new Long(values[1]);
			byte[] lng = new byte[] {
				(byte) (newUid >> 56),
				(byte) (newUid >> 48),
				(byte) (newUid >> 40),
				(byte) (newUid >> 32),
				(byte) (newUid >> 24),
				(byte) (newUid >> 16),
				(byte) (newUid >> 8),
				(byte) newUid
			};
			byte[] classname = values[0].getBytes();
			byte[] payload = new byte[classname.length + lng.length];
			// byte[] concat classname|uid
			for (int i = 0; i < payload.length; i++) {
				payload[i] = i < classname.length ? classname[i] : lng[i - classname.length];
			}
			// byte[] sed s/classname..../classnameSUID/g
			int acc = 0;
			for (int i = 0; i < bytes.length; i++) {
				if(bytes[i] == classname[acc]) {
					acc++;
				} else {
					acc = 0;
				}
				if(acc == classname.length) {
					for (int z = 0; z < 8; z++) {
						i++;
						bytes[i] = lng[z];
					}
					acc = 0;
				}
			}
		}
		out.write(bytes);
	}
}