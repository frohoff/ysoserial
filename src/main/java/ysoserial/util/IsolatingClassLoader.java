package ysoserial.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class IsolatingClassLoader extends URLClassLoader {
	
	private Map<String, byte[]> classes;
	private ClassLoader utilLoader;

	public IsolatingClassLoader( ClassLoader utilLoader ) {
		super(new URL[0], String.class.getClassLoader());
		this.classes = new HashMap<String, byte[]>();
		this.utilLoader = utilLoader;
	}
	
	public void addClass( String name, InputStream bytes ) throws IOException { 
		byte[] byteArray = new byte[bytes.available()];
		bytes.read( byteArray );
		
		this.classes.put( name, byteArray );
	}
	
	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> cls = null;
		
		// Statically defined payload classes (for inclusion)
		if ( classes.containsKey( name ) ) { 
			super.defineClass( name, classes.get(name), 0, classes.get(name).length );
		} else if ( name.startsWith( "javax." ) || name.startsWith( "java." ) ) {
			cls = utilLoader.loadClass( name );
		}
		
		if ( cls == null ) { 
			try { 
				cls = super.loadClass( name, false );
			} catch( ClassNotFoundException e ) { 
				if ( cls == null ) { 
					cls = this.utilLoader.loadClass( name );
				}
			}
		}

		if ( cls == null ) { 
			// Should never happen, because this should have been thrown by utilLoader
			throw new ClassNotFoundException( name );
		}

		String[] deps = DependencyUtil.getDependencies(cls);
		
		if ( deps.length > 0 ) {
	        File[] jars = deps.length > 0 ? Maven.resolver().resolve(deps).withoutTransitivity().asFile() : new File[0];
	        for ( int i = 0; i < jars.length; i++ ) {
	        	try { 
	        		super.addURL( jars[ i ].toURI().toURL() );
	        	} catch( MalformedURLException e ) {
	        		e.printStackTrace();
	        	}
	        }
		}
				
		if ( resolve ) { 
			resolveClass( cls );
		}
		
		return cls;
	}

}
