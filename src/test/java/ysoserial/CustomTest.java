/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 05.03.2016 by mbechler
 */
package ysoserial;

import java.util.concurrent.Callable;

/**
 * @author mbechler
 *
 */
public interface CustomTest  {

    void run (Callable<Object> payload) throws Exception;

    String getPayloadArgs ();
}
