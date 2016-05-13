package ysoserial.payloads;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import ysoserial.GeneratePayload;
import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.interfaces.ReleaseableObjectPayload;

@SuppressWarnings ( "rawtypes" )
public class Utils {
	
	public static void wire( ObjectPayload payload, String[] arguments ) throws Exception {
		
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

	private static void resolve(ObjectPayload payload, Map<String, String> params) throws Exception {
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
					
					ObjectPayload<?> newPayload = getPayloadClass( thisValue ).newInstance();
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

	private static Map<String, Field> getBindableFields(ObjectPayload payload) {
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
        final Class<? extends ObjectPayload> payloadClass = getPayloadClass(payloadType);
        if ( payloadClass == null || !ObjectPayload.class.isAssignableFrom(payloadClass) ) {
            throw new IllegalArgumentException("Invalid payload type '" + payloadType + "'");

        }

        final Object payloadObject;
        try {
            final ObjectPayload payload = payloadClass.newInstance();
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
}