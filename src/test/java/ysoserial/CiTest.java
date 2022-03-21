package ysoserial;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

public class CiTest {
    @Test
    public void test() {
        for (Map.Entry<Object,Object> e : new TreeMap<Object,Object>(System.getProperties()).entrySet()) {
            System.out.println("System property " + e.getKey() + " : " + e.getValue());
        }
        for (Map.Entry<String,String> e : new TreeMap<String,String>(System.getenv()).entrySet()) {
            System.out.println("System env " + e.getKey() + " : " + e.getValue());
        }
    }

    public static void main(String[] args) {
        new CiTest().test();
    }
}
