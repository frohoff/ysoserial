package ysoserial.test.payloads;

import org.junit.Assert;
import sun.security.util.SecurityConstants;
import ysoserial.payloads.URLDNS;
import ysoserial.secmgr.SecurityManagers;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Randomized;
import ysoserial.test.util.RecordingNameService;
import ysoserial.test.util.RecordingSecurityManager;

import java.net.SocketPermission;
import java.security.Permission;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class DnsLookupTest implements CustomTest {
    private final String testDomain = Randomized.randUUID();

    @Override
    public void run(Callable<Object> payload) throws Exception {
//        RecordingSecurityManager sm = new RecordingSecurityManager();

        RecordingNameService ns = new RecordingNameService();
//        Assert.assertFalse("should not have resolved domain",
//            sm.getChecks().contains(new SocketPermission(testDomain, SecurityConstants.SOCKET_RESOLVE_ACTION)));
        Assert.assertFalse("should not have resolved domain", ns.getLookups().contains(testDomain));
        try {
//            SecurityManagers.wrapped(payload, sm).call();
            ns.install();
            payload.call();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ns.uninstall();
        }


        Assert.assertTrue("should have resolved domain", ns.getLookups().contains(testDomain));
//        Assert.assertTrue("should have resolved domain",
//            sm.getChecks().contains(new SocketPermission(testDomain, SecurityConstants.SOCKET_RESOLVE_ACTION)));
    }

    @Override
    public String getPayloadArgs() {
        return "http://" + testDomain;
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(URLDNS.class);
    }

}
