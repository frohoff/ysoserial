package ysoserial.util;

import java.util.Arrays;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.DynamicDependencies;
import ysoserial.payloads.annotation.DynamicDependencies.Condition;

public class DependencyUtil {
	
	public static String[] getDependencies(Class<?> cls) {
		String[] deps = new String[0];
				
		if ( cls.getAnnotation( Dependencies.class ) != null ) {
			Dependencies annotationDeps = cls.getAnnotation( Dependencies.class );
			deps = annotationDeps.value();
		}
		
		if ( cls.getAnnotation( DynamicDependencies.class ) != null ) {
			DynamicDependencies annotationDeps = cls.getAnnotation( DynamicDependencies.class );
			Binding binding = new Binding();
			binding.setVariable("arg", Arguments.peek());
			
			GroovyShell shell = new GroovyShell( binding );
						
			for( Condition c : annotationDeps.value() ) { 
				boolean matches = true;
				if ( c.condition() != null && !c.condition().isEmpty() ) { 
					Boolean outcome = (Boolean)shell.evaluate( c.condition() );
					if ( outcome == null || !outcome.booleanValue() ) {
						matches = false;
					}
				}
								
				if ( matches ) {
					int additional = c.deps().value().length;
					deps = Arrays.copyOf( deps, deps.length + additional );
					System.arraycopy( c.deps().value(), 0, deps, deps.length - additional, additional );
					break;
				}
			}
		}
		
        if ( System.getProperty("properXalan") != null ) {
            deps = Arrays.copyOf(deps, deps.length + 1);
            deps[ deps.length - 1 ] = "xalan:xalan:2.7.2";
        }
		return deps;
	}
	


}
