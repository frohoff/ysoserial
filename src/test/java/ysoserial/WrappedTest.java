package ysoserial;

import java.util.concurrent.Callable;

/**
 * @author mbechler
 *
 */
public interface WrappedTest {

    Callable<Object> createCallable ( Callable<Object> innerCallable );
    
}
