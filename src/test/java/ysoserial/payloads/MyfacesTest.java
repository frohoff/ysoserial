package ysoserial.payloads;


import java.beans.FeatureDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.MapELResolver;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.myfaces.el.CompositeELResolver;
import org.apache.myfaces.el.unified.FacesELContext;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ysoserial.CustomDeserializer;
import ysoserial.Deserializer;


/**
 * @author mbechler
 *
 */
public class MyfacesTest extends RemoteClassLoadingTest implements CustomDeserializer {

    /**
     * @param command
     */
    public MyfacesTest ( String command ) {
        super(command);
    }


    /**
     * {@inheritDoc}
     *
     * @see ysoserial.CustomDeserializer#getCustomDeserializer()
     */
    public Class<?> getCustomDeserializer () {
        return MyfacesDeserializer.class;
    }

    /**
     * need to use a custom deserializer so that the faces context gets set in the isolated class
     * 
     * @author mbechler
     *
     */
    public static final class MyfacesDeserializer extends Deserializer {

        public static Class<?>[] getExtraDependencies () {
            return new Class[] {
                MockRequestContext.class, MockELResolver.class
            };
        }

        private static class MockRequestContext implements Answer<Object> {

            private Map<String, Object> attributes = new HashMap<String, Object>();


            /**
             * {@inheritDoc}
             *
             * @see org.mockito.stubbing.Answer#answer(org.mockito.invocation.InvocationOnMock)
             */
            public Object answer ( InvocationOnMock invocation ) throws Throwable {

                if ( "setAttribute".equals(invocation.getMethod().getName()) ) {
                    this.attributes.put(invocation.getArgumentAt(0, String.class), invocation.getArgumentAt(1, Object.class));
                    return null;
                }
                else if ( "getAttribute".equals(invocation.getMethod().getName()) ) {
                    return this.attributes.get(invocation.getArgumentAt(0, String.class));
                }
                return null;
            }

        }


        private static class MockELResolver extends ELResolver {
            
            private ServletRequest request;
            
            
            /**
             * 
             */
            public MockELResolver (ServletRequest req) {
                this.request = req;
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#getValue(javax.el.ELContext, java.lang.Object, java.lang.Object)
              */
            @Override
            public Object getValue ( ELContext context, Object base, Object property ) {
                if ( base == null && "request".equals(property)) {
                    context.setPropertyResolved(true);
                    return this.request;
                }
                
                return null;
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#getType(javax.el.ELContext, java.lang.Object, java.lang.Object)
              */
            @Override
            public Class<?> getType ( ELContext context, Object base, Object property ) {
                if ( base == null && "request".equals(property)) {
                    context.setPropertyResolved(true);
                    return ServletRequest.class;
                }
                return null;
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#setValue(javax.el.ELContext, java.lang.Object, java.lang.Object, java.lang.Object)
              */
            @Override
            public void setValue ( ELContext context, Object base, Object property, Object value ) {
        
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#isReadOnly(javax.el.ELContext, java.lang.Object, java.lang.Object)
              */
            @Override
            public boolean isReadOnly ( ELContext context, Object base, Object property ) {
                return true;
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#getFeatureDescriptors(javax.el.ELContext, java.lang.Object)
              */
            @Override
            public Iterator<FeatureDescriptor> getFeatureDescriptors ( ELContext context, Object base ) {
                return null;
            }

            /**
              * {@inheritDoc}
              *
              * @see javax.el.ELResolver#getCommonPropertyType(javax.el.ELContext, java.lang.Object)
              */
            @Override
            public Class<?> getCommonPropertyType ( ELContext context, Object base ) {
                return null;
            }
            
        }
        
        /**
         * @param bytes
         */
        public MyfacesDeserializer ( byte[] bytes ) {
            super(bytes);
        }


        @Override
        public Object call () throws Exception {
            java.lang.reflect.Method setFC = FacesContext.class.getDeclaredMethod("setCurrentInstance", FacesContext.class);
            setFC.setAccessible(true);
            ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            FacesContext ctx = createMockFacesContext();
            try {
                setFC.invoke(null, ctx);
                return super.call();
            }
            finally {
                setFC.invoke(null, (FacesContext) null);
                Thread.currentThread().setContextClassLoader(oldTCCL);
            }
        }


        /**
         * @return
         * @throws MalformedURLException
         */
        private static FacesContext createMockFacesContext () throws MalformedURLException {
            FacesContext ctx = Mockito.mock(FacesContext.class);
            CompositeELResolver cer = new CompositeELResolver();
            FacesELContext elc = new FacesELContext(cer, ctx);
            ServletRequest requestMock = Mockito.mock(ServletRequest.class);
            ServletContext contextMock = Mockito.mock(ServletContext.class);
            URL url = new URL("file:///");
            Mockito.when(contextMock.getResource(Matchers.anyString())).thenReturn(url);
            Mockito.when(requestMock.getServletContext()).thenReturn(contextMock);
            Answer<?> attrContext = new MockRequestContext();
            Mockito.when(requestMock.getAttribute(Matchers.anyString())).thenAnswer(attrContext);
            Mockito.doAnswer(attrContext).when(requestMock).setAttribute(Matchers.anyString(), Matchers.any());
            cer.add(new MockELResolver(requestMock));
            cer.add(new BeanELResolver());
            cer.add(new MapELResolver());
            Mockito.when(ctx.getELContext()).thenReturn(elc);
            return ctx;
        }
    }

}
