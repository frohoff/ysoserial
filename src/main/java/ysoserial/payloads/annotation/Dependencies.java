package ysoserial.payloads.annotation;

import ysoserial.payloads.DynamicDependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependencies {
	String[] value() default {};

	public static class Utils {
		public static String[] getDependencies(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            if ( DynamicDependencies.class.isAssignableFrom(clazz) ) {
                Method method = clazz.getMethod("getDependencies");
                return (String[]) method.invoke(null);
            }
			Dependencies deps = clazz.getAnnotation(Dependencies.class);
			if (deps != null && deps.value() != null) {
				return deps.value();
			} else {
				return new String[0];
			}
		}

		public static String[] getDependenciesSimple(Class<?> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		    String[] deps = getDependencies(clazz);
		    String[] simple = new String[deps.length];
		    for (int i = 0; i < simple.length; i++) {
                simple[i] = deps[i].split(":", 2)[1];
            }
            return simple;
        }
	}
}
