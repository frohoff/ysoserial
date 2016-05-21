package ysoserial.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.grape.Grape;
import groovy.lang.GroovyClassLoader;

public class IsolatingGroovyClassLoader extends GroovyClassLoader {
	
	private Map<String, byte[]> classes;
	private ClassLoader utilLoader;

	public IsolatingGroovyClassLoader( ClassLoader utilLoader ) {
		super(String.class.getClassLoader());
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
			super.defineClass( name, classes.get(name) );
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
			List<Map<String, String>> dependencies = new ArrayList<Map<String, String>>();
			Map<String, Object> args = new HashMap<String, Object>();
			args.put( "classLoader", this );
			for( String dep : deps ) { 
				Messages.println( "Including dependency {0}", dep );
				String[] depPieces = dep.split( ":" );
				if ( depPieces.length > 1 ) { 
					Map<String, String> dependency = new HashMap<String, String>();

					dependency.put( "group", depPieces[0] );
					dependency.put( "module", depPieces[1] );
					
					if ( depPieces.length == 3 ) { 
						dependency.put( "version", depPieces[2] );
					} else if ( depPieces.length == 5 ) { 
						dependency.put( "version", depPieces[4] );
						dependency.put( "classifier", depPieces[3] );
						dependency.put( "type", depPieces[2] );
					}
					dependencies.add( dependency );
				}
			}


			Grape.setEnableAutoDownload(true);
			Grape.grab( args, dependencies.toArray( new Map[0] ) );
		}
				
		if ( resolve ) { 
			resolveClass( cls );
		}
		
		return cls;
	}

}
