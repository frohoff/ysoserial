package ysoserial.test.util;

import org.junit.Test;
import ysoserial.payloads.util.Gadgets;

public class GadgetsTest {
    @Test
    public void test_createTemplatesImpl_noCompilationError() throws Exception {
        Gadgets.createTemplatesImpl("hostname");
        Gadgets.createTemplatesImpl("echo 'foobar'");
        Gadgets.createTemplatesImpl("echo \"foobar\"");
        Gadgets.createTemplatesImpl("\"`';\\");
    }
}
