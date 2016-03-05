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
