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
public interface WrappedTest {

    /**
     * @param innerCallable
     * @return a wrapped callable
     */
    Callable<Object> createCallable ( Callable<Object> innerCallable );
    
}
