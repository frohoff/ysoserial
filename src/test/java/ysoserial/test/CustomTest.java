package ysoserial.test;

import java.util.concurrent.Callable;

/**
 * @author mbechler
 *
 */
public interface CustomTest extends CustomPayloadArgs {

    void run (Callable<Object> payload) throws Exception;
}
