package ysoserial.test.payloads;

import org.junit.Assert;
import ysoserial.payloads.URLDNS;
import ysoserial.secmgr.SecurityManagers;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Randomized;

import java.security.Permission;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class DnsLookupTest implements CustomTest {
    private final String testDomain = Randomized.randUUID();

    @Override
    public void run(Callable<Object> payload) throws Exception {
        final List<String> lookups = new LinkedList<String>();
        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkConnect(String host, int port) {
                if (port == -1) {
                    System.out.println(host);
                    lookups.add(host);
                }
            }

            @Override
            public void checkPermission(Permission perm) {}
        };

        try {
            SecurityManagers.wrapped(payload, sm).call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertTrue(lookups.contains(testDomain));
    }

    @Override
    public String getPayloadArgs() {
        return "http://" + testDomain;
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(URLDNS.class, new Class[0]);
    }

}
