package ysoserial.payloads;


import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import org.reflections.Reflections;

import ysoserial.GeneratePayload;


@SuppressWarnings ( "rawtypes" )
public interface ObjectPayload <T> {

    /*
     * return armed payload object to be serialized that will execute specified
     * command on deserialization
     */
    public T getObject ( String command ) throws Exception;

    public static class Utils {

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
                payloadObject = payload.getObject(payloadArg);
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
}
