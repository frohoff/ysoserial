/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 06.03.2016 by mbechler
 */
package ysoserial.payloads;


/**
 * @author mbechler
 *
 */
public interface ReleaseableObjectPayload<T> extends ObjectPayload<T> {

    void release( T obj ) throws Exception;
}
