package ysoserial.test.payloads;

import ysoserial.payloads.Myfaces3;
import ysoserial.test.CustomDeserializer;

public class MyFacesExecTest extends CommandExecTest implements CustomDeserializer  {
    // FIXME replace CustomDeserializer with inner payload wrapper (w/ limited classloader)
    public Class<?> getCustomDeserializer () {
        return MyfacesTest.MyfacesDeserializer.class;
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(Myfaces3.class);
    }
}
