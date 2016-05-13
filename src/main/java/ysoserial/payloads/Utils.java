package ysoserial.payloads;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import ysoserial.GeneratePayload;
import ysoserial.annotation.Bind;
import ysoserial.annotation.PayloadTypes;
import ysoserial.annotation.PayloadTypes.Type;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.interfaces.ReleaseableObjectPayload;

@SuppressWarnings ( "rawtypes" )
public class Utils {
	
	public static void wire( Object payload, String[] arguments ) throws Exception {
		
		Map<String, String> params = new HashMap<String, String>();
		if ( arguments.length == 1 ) {
			params.put( "command", arguments[0] );
		} else {
			for( int i = 0; i < arguments.length; i += 2 ) {
				String argName = arguments[i];
				String argVal = arguments[i + 1];
				
				if ( argName.startsWith( "-" ) ) {
					argName = argName.substring( 1 );
				}
				
				if ( argVal.startsWith( "-" ) ) {
					// This is a boolean "presence" argument
					i--;
					params.put( argName, "true" );
				} else {
					// This is a regular argument
					params.put( argName, argVal );
				}
			}
		}
		
		resolve(payload, params);
	}

	private static void resolve(Object payload, Map<String, String> params) throws Exception {
		Map<String, Field> bindableFields = getBindableFields( payload );
		
		// Copy to avoid concurrent modification exception
		Set<String> paramKeys = new HashSet<String>(params.keySet());

		// Resolve and bind
		for( String thisKey : paramKeys ) { 
			if ( bindableFields.isEmpty() ) {
				break;
			}
			
			String field = "";
			
			for( String fieldName : bindableFields.keySet() ) {
				if ( fieldName.equals( thisKey ) ) {
					field = fieldName;
					break;
				}
				if ( fieldName.startsWith( thisKey ) ) {
					if ( !field.isEmpty() ) {
						throw new IllegalArgumentException( "Argument '" + thisKey + "' is not unambiguous" );
					}
					field = fieldName;
				}
			}
			
			if ( field.isEmpty() ) {
				// this is probably a sub-field or a typo
				continue;
			}

			try { 
				Field thisField = bindableFields.remove( field );
				String thisValue = params.get(thisKey);
				
				String[] allowed = thisField.getAnnotation( Bind.class ).allowed();
				if ( allowed.length > 0 ) {
					boolean found = false;
					for( int a = 0; a < allowed.length; a++ ) {
						if ( thisValue.equals( allowed ) ) {
							found = true;
							break;
						}
					}
					
					if ( !found ) {
						// TODO: Print an actual list of the valid values
						throw new IllegalArgumentException( "Value '" + thisValue + "' is not one of the permitted values for '" + thisField.getName() + "'" );
					}
				}
				
				String[] forbidden = thisField.getAnnotation( Bind.class ).forbidden();
				if ( forbidden.length > 0 ) {
					for( int f = 0; f < forbidden.length; f++ ) {
						if ( thisValue.equals( forbidden ) ) {
							throw new IllegalArgumentException( "Value '" + thisValue + "' is forbidden for field '" + thisField.getName() + "'" );
						}
					}
				}
				
				Class<?> type = thisField.getType();				
				if ( type.equals( String.class ) ) {
					thisField.set( payload, thisValue );
				} else if ( type.equals( URL.class ) ) {
					thisField.set( payload, new URL(thisValue) );
				} else if ( type.equals( Integer.TYPE ) ) {
					thisField.setInt( payload, Integer.parseInt( thisValue ) );
				} else if ( type.equals( Long.TYPE ) ) {
					thisField.setLong( payload, Integer.parseInt( thisValue ) );
				} else if ( type.equals( Boolean.TYPE ) ) {
					thisField.setBoolean( payload, isFlagSet( thisValue ) );
				} else if ( type.equals( File.class ) ) { 
					thisField.set( payload, new File( thisValue ) );
				} else if ( type.equals( FileInputStream.class ) ) {
					thisField.set( payload, new FileInputStream( new File( thisValue ) ) );
				} else if ( type.equals( ObjectPayload.class ) || ObjectPayload.class.isAssignableFrom( type ) ) {
					int len = (thisKey + ".").length();
					// Sub-payload!
					Set<String> toRemove = new HashSet<String>();
					Map<String, String> subArgs = new HashMap<String, String>();
					for( String param : params.keySet() ) {
						if ( param.startsWith( thisKey + "." ) ) { 
							subArgs.put( param.substring( len ), params.get(param) );
							toRemove.add( param );
						}
					}
					
					for( String rm : toRemove ) {
						params.remove( rm );
					}
					Class<? extends ObjectPayload> payloadClass = getPayloadClass( thisValue );
					
					checkBindableTypes(thisField, thisValue, payloadClass);
					
					ObjectPayload<?> newPayload = payloadClass.newInstance();
					resolve( newPayload, subArgs );
					
					thisField.set( payload, newPayload );
				}
				
			} finally {
				params.remove( thisKey );
			}
		}
		
		if ( !bindableFields.isEmpty() && params.isEmpty() ) {
			for( String name: bindableFields.keySet() ) {
				Field f = bindableFields.get( name );
				Class<?> type = f.getType();
				if ( !"".equals( f.getAnnotation(Bind.class).defaultValue() ) ) {
					if ( type.equals( String.class ) ) {
						f.set( payload, f.getAnnotation(Bind.class).defaultValue() );
					} else if ( type.equals( URL.class ) ) {
						f.set( payload, new URL(f.getAnnotation(Bind.class).defaultValue()) );
					} else if ( type.equals( Integer.TYPE ) ) {
						f.setInt( payload, Integer.parseInt( f.getAnnotation(Bind.class).defaultValue() ) );
					} else if ( type.equals( Long.TYPE ) ) {
						f.setLong( payload, Integer.parseInt( f.getAnnotation(Bind.class).defaultValue() ) );
					} else if ( type.equals( Boolean.TYPE ) ) {
						f.setBoolean( payload, isFlagSet( f.getAnnotation(Bind.class).defaultValue() ) );
					} else {
						throw new IllegalStateException( "Configuration error: default bindings cannot be used for " + type );
					}
				} else {
					throw new IllegalArgumentException( "Missing value for field: " + name );
				}
			}
		}
	}

	private static void checkBindableTypes(Field thisField, String thisValue,
			Class<? extends ObjectPayload> payloadClass) {
		Type[] types = new Type[] { Type.Remote_Code_Execution };
		// Filter types if necessary
		if ( payloadClass.getAnnotation( PayloadTypes.class ) != null ) { 
			types = payloadClass.getAnnotation( PayloadTypes.class ).value();
		}

		List<Type> allowedTypes = Arrays.asList( thisField.getAnnotation(Bind.class).allowedTypes() );
		List<Type> forbiddenTypes = Arrays.asList( thisField.getAnnotation(Bind.class).forbiddenTypes() );
		
		boolean isAllowed = ( allowedTypes.size() == 0);
		boolean isDenied = (forbiddenTypes.size() > 0);
		for( int t = 0; t < types.length; t++ ) {
			if ( allowedTypes.size() > 0 && allowedTypes.contains( types[t] ) ) {
				isAllowed = true;
			}
			
			if ( forbiddenTypes.size() > 0 && forbiddenTypes.contains( types[t] ) ) {
				isDenied = true;
			}
		}
		
		if ( !isAllowed || isDenied ) {
			throw new IllegalArgumentException( "Payload type '" + thisValue + "' is not allowed here" );
		}
	}
	
    private static boolean isFlagSet(String thisValue) {
    	if ( thisValue == null || thisValue.isEmpty() ) {
    		return false;
    	}
    	
    	if ( thisValue.equalsIgnoreCase( "true" ) ) {
    		return true;
    	} else if ( thisValue.equalsIgnoreCase( "y" ) ) { 
    		return true;
    	} else if ( thisValue.equalsIgnoreCase( "yes" ) ) {
    		return true;
    	} else if ( thisValue.equalsIgnoreCase( "t" ) ) {
    		return true;
    	} else if ( thisValue.equals( "1" ) ) {
    		return true;
    	}
		return false;
	}

	private static Map<String, Field> getBindableFields(Object payload) {
    	Map<String, Field> fields = new HashMap<String, Field>();
    	
    	Class<?> payloadCls = payload.getClass();
    	
    	while( payloadCls != null ) { 
	    	for( Field f : payloadCls.getDeclaredFields() ) { 
	    		if ( f.getAnnotation( Bind.class ) != null ) {
	    			f.setAccessible( true );
	    			fields.put( f.getName(), f );
	    		}
	    	}
	    	payloadCls = payloadCls.getSuperclass();
    	}
    	
		return fields;
	}

	// get payload classes by classpath scanning
    public static Set<Class<? extends ObjectPayload>> getPayloadClasses () {
        final Reflections reflections = new Reflections(ObjectPayload.class.getPackage().getName());
        final Set<Class<? extends ObjectPayload>> payloadTypes = reflections.getSubTypesOf(ObjectPayload.class);
        for ( Iterator<Class<? extends ObjectPayload>> iterator = payloadTypes.iterator(); iterator.hasNext(); ) {
            Class<? extends ObjectPayload> pc = iterator.next();
            if ( pc.isInterface() || Modifier.isAbstract(pc.getModifiers()) ) {
                iterator.remove();
            }
        }
        return payloadTypes;
    }


    @SuppressWarnings ( "unchecked" )
    public static Class<? extends ObjectPayload> getPayloadClass ( final String className ) {
        Class<? extends ObjectPayload> clazz = null;
        try {
            clazz = (Class<? extends ObjectPayload>) Class.forName(className);
        }
        catch ( Exception e1 ) {}
        if ( clazz == null ) {
            try {
                return clazz = (Class<? extends ObjectPayload>) Class
                        .forName(GeneratePayload.class.getPackage().getName() + ".payloads." + className);
            }
            catch ( Exception e2 ) {}
        }
        if ( clazz != null && !ObjectPayload.class.isAssignableFrom(clazz) ) {
            clazz = null;
        }
        return clazz;
    }


    public static Object makePayloadObject ( String payloadType, String payloadArg ) {
    	return makePayloadObject( payloadType, new String[] { "command", payloadArg } );
    }
    
    public static Object makePayloadObject( String payloadType, String[] payloadArgs ) { 
        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(payloadType);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            throw new IllegalArgumentException("Invalid payload type '" + payloadType + "'");
        }

        final Object payloadObject;
        try {
            final ObjectPayload payload = payloadClass.newInstance();
            wire( payload, payloadArgs );
            payloadObject = payload.getObject();
        }
        catch ( Exception e ) {
            throw new IllegalArgumentException("Failed to construct payload", e);
        }
        return payloadObject;
    }


    @SuppressWarnings ( "unchecked" )
    public static void releasePayload ( ObjectPayload payload, Object object ) throws Exception {
        if ( payload instanceof ReleaseableObjectPayload ) {
            ( (ReleaseableObjectPayload) payload ).release(object);
        }
    }


    public static void releasePayload ( String payloadType, Object payloadObject ) {
        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(payloadType);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            throw new IllegalArgumentException("Invalid payload type '" + payloadType + "'");

        }

        try {
            final ObjectPayload payload = payloadClass.newInstance();
            releasePayload(payload, payloadObject);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

    }

	public static String[] trimArgs(String[] args, int i) {
		if ( args.length - i < 0 ) { 
			throw new IllegalArgumentException( "Can't trim more than the number of elements off an array" );
		}
		String[] newArgs = new String[ args.length - i ];
		
		// Zero is technically a valid number of arguments, so we'll allow it
		if ( newArgs.length > 0 ) { 
			System.arraycopy( args, i, newArgs, 0, newArgs.length );
		}
		
		return newArgs;
	}
}