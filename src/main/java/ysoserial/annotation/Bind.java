package ysoserial.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD, ElementType.TYPE } )
public @interface Bind {

	/**
	 * All allowed String values for this Bind variable. If not populated, or empty, the wiring
	 * code will allow all values.
	 */
	String[] allowed() default {};
	
	/**
	 * If this is an ObjectPayload variable, this specifies the allowed types of payloads. For other
	 * types, this value is ignored. Classes not tagged with a type will be assumed to be type
	 * Remote_Code_Execution.
	 */
	PayloadTypes.Type[] allowedTypes() default {};
	
	/**
	 * The default value for this Bind variable, which effectively makes it
	 * optional if populated. Any fields not populated by command line switches
	 * will be populated by their default values.
	 */
	String defaultValue() default "";
	
	/**
	 * Any forbidden String values for this Bind variable
	 */
	String[] forbidden() default {};

	/**
	 * If this is an ObjectPayload variable, this specifies the forbidden types of payloads. For other
	 * types, this value is ignored.
	 */
	PayloadTypes.Type[] forbiddenTypes() default {};
	
	/**
	 * Printed if the payload or exploit is not validly used
	 */
	String helpText() default "";

	
}
