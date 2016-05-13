package ysoserial.annotation;

public @interface PayloadTypes {
	
	public static enum Type {
		Code_Injection,
		Denial_of_Service,
		Remote_Code_Execution,
		Reverse,
		Shell,
		Wrapper
	}
	
	Type[] value() default {};

}
