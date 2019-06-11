package ysoserial.payloads.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authors {
    String FROHOFF = "frohoff";
    String PWNTESTER = "pwntester";
    String CSCHNEIDER4711 = "cschneider4711";
    String MBECHLER = "mbechler";
    String JACKOFMOSTTRADES = "JackOfMostTrades";
    String MATTHIASKAISER = "matthias_kaiser";
    String GEBL = "gebl" ;
    String JACOBAINES = "jacob-baines";
    String JASINNER = "jasinner";
    String KULLRICH = "kai_ullrich";
    String TINT0 = "_tint0";
    String SCRISTALLI = "scristalli";
    String HANYRAX = "hanyrax";
    String EDOARDOVIGNATI = "EdoardoVignati";

    String[] value() default {};

    public static class Utils {
        public static String[] getAuthors(AnnotatedElement annotated) {
            Authors authors = annotated.getAnnotation(Authors.class);
            if (authors != null && authors.value() != null) {
                return authors.value();
            } else {
                return new String[0];
            }
        }
    }
}
