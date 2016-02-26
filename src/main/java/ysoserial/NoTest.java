/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 25.02.2016 by mbechler
 */
package ysoserial;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author mbechler
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NoTest {

    @SuppressWarnings ( "javadoc" )
    String value() default "";
}
