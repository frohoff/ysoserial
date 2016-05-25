package ysoserial.util;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tiny wrapper to make reflection a little bit easier.
 */
public class Reflected {
	
	private Class<?> cls;
	private Object obj;
	
	public Reflected( String clsName ) throws ClassNotFoundException {
		this( Class.forName( clsName ) );
	}

	public Reflected( Class<?> cls ) {
		this.cls = cls;
	}
	
	public Reflected( Object obj ) {
		this.cls = obj.getClass();
		this.obj = obj;
	}
	
	public Reflected call( String methodName, Object... params ) throws Exception {
		Class<?>[] paramClasses = new Class<?>[ params.length ];
		for( int i = 0; i < paramClasses.length; i++ ) {
			paramClasses[i] = params[i].getClass();
		}
		
		@SuppressWarnings("unchecked")
		Set<Method> results = getAllMethods( cls, withName( methodName ), params.length == 0 ? withParametersCount( 0 ) : withParametersAssignableTo( paramClasses ) );
		
		Method method = null;
		
		if ( results.size() != 1 ) {
			if ( results.size() > 1 ) { 
				for( Method m : results ) {
					if ( m.getDeclaringClass().equals( cls ) ) {
						method = m;
						break;
					} else if ( ( m.getModifiers() & Modifier.ABSTRACT ) == 0 ) {
						method = m;
						break;
					}
				}
			}
			if ( method == null ) { 
				throw new IllegalArgumentException( "Method called " + methodName + " not found for parameter types " + Arrays.asList( paramClasses ) + " on type " + this.cls  );
			}
		} else {
			method = results.iterator().next();
		}
		
		try { 
			return wrap( method.invoke( this.obj, params ) );
		} catch( InvocationTargetException e ) {
			if ( e.getCause() instanceof Exception ) {
				throw (Exception)e.getCause();
			}
			
			throw e;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Reflected build( Object... constructorParams ) throws Exception {
		Class<?>[] paramClasses = new Class<?>[ constructorParams.length ];
		for( int i = 0; i < paramClasses.length; i++ ) {
			paramClasses[i] = constructorParams[i].getClass();
		}
		
		@SuppressWarnings("rawtypes")
		Set<Constructor> constructors = getAllConstructors( this.cls, withParametersAssignableTo( paramClasses ) );
		if ( constructors.size() != 1 ) {
			throw new IllegalArgumentException( "Could not find constructor for parameter types " + Arrays.asList( paramClasses ) );
		}
		
		@SuppressWarnings("rawtypes")
		Constructor constructor = constructors.iterator().next();
		
		return wrap( constructor.newInstance( constructorParams ) );
	}
	
	public String toString() { 
		if ( obj != null ) {
			return String.valueOf( obj );
		} else {
			return "Class: " + this.cls.getName();
		}
	}

	private Reflected wrap(Object toWrap) {
		return new Reflected( toWrap );
	}

	public static void main( String args[] ) throws Exception { 
		List<String> original = Arrays.asList( "a", "b", "c" );
		Reflected refl = new Reflected( "java.util.ArrayList" );
		System.out.println( refl.build( original ).call("size") );
	}
	
}
