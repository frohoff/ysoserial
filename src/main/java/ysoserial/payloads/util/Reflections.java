package ysoserial.payloads.util;

import java.lang.reflect.*;

import sun.reflect.ReflectionFactory;

import com.nqzero.permit.Permit;

@SuppressWarnings ( "restriction" )
public class Reflections {

    public static void setAccessible(AccessibleObject member) {
        String versionStr = System.getProperty("java.version");
        int javaVersion = Integer.parseInt(versionStr.split("\\.")[0]);
        if (javaVersion < 12) {
          // quiet runtime warnings from JDK9+
          Permit.setAccessible(member);
        } else {
          // not possible to quiet runtime warnings anymore...
          // see https://bugs.openjdk.java.net/browse/JDK-8210522
          // to understand impact on Permit (i.e. it does not work
          // anymore with Java >= 12)
          member.setAccessible(true);
        }
    }

	public static Field getField(final Class<?> clazz, final String fieldName) {
        Field field = null;
	    try {
            field = clazz.getDeclaredField(fieldName);
            setAccessible(field);
        } catch (NoSuchFieldException ex) {
            if (clazz.getSuperclass() != null)
                field = getField(clazz.getSuperclass(), fieldName);
        }
		return field;
	}

	public static void setFieldValue(Object obj, final String fieldName, final Object value) throws Exception {
        Class clazz = obj instanceof Class ? (Class) obj : obj.getClass();
        obj = obj instanceof Class ? null : obj;
		final Field field = getField(clazz, fieldName);

        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(obj, value);
	}

	public static Object getFieldValue(Object obj, final String fieldName) throws Exception {
        Class clazz = obj instanceof Class ? (Class) obj : obj.getClass();
        obj = obj instanceof Class ? null : obj;
		final Field field = getField(clazz, fieldName);
		return field.get(obj);
	}

    public static Object getFieldValues(Object obj, final String ... fieldNames) throws Exception {
        for (String fieldName : fieldNames) {
            if (obj == null) {
                throw new NullPointerException();
            }
            obj = getFieldValue(obj, fieldName);
        }
        return obj;
    }

	public static Constructor<?> getFirstCtor(final String name) throws Exception {
		final Constructor<?> ctor = Class.forName(name).getDeclaredConstructors()[0];
	    setAccessible(ctor);
	    return ctor;
	}

	public static Object newInstance(String className, Object ... args) throws Exception {
        return getFirstCtor(className).newInstance(args);
    }

    public static <T> T createWithoutConstructor ( Class<T> classToInstantiate )
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return createWithConstructor(classToInstantiate, Object.class, new Class[0], new Object[0]);
    }

    @SuppressWarnings ( {"unchecked"} )
    public static <T> T createWithConstructor ( Class<T> classToInstantiate, Class<? super T> constructorClass, Class<?>[] consArgTypes, Object[] consArgs )
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<? super T> objCons = constructorClass.getDeclaredConstructor(consArgTypes);
	    setAccessible(objCons);
        Constructor<?> sc = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(classToInstantiate, objCons);
	    setAccessible(sc);
        return (T)sc.newInstance(consArgs);
    }

}
