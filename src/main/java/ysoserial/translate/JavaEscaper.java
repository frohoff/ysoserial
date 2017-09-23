package ysoserial.translate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JavaEscaper {
    public static final Map<CharSequence, CharSequence> JAVA_CTRL_CHARS_ESCAPE;
    public static final CharSequenceTranslator ESCAPE_JAVA;

    static {
        Map<CharSequence, CharSequence> initialMap = new HashMap<CharSequence, CharSequence>();
        initialMap.put("\b", "\\b");
        initialMap.put("\n", "\\n");
        initialMap.put("\t", "\\t");
        initialMap.put("\f", "\\f");
        initialMap.put("\r", "\\r");
        JAVA_CTRL_CHARS_ESCAPE = Collections.unmodifiableMap(initialMap);
    	
        Map<CharSequence, CharSequence> escapeJavaMap = new HashMap<CharSequence, CharSequence>();
        escapeJavaMap.put("\"", "\\\"");
        escapeJavaMap.put("\\", "\\\\");
        ESCAPE_JAVA = new AggregateTranslator(
                new LookupTranslator(Collections.unmodifiableMap(escapeJavaMap)),
                new LookupTranslator(JAVA_CTRL_CHARS_ESCAPE),
                JavaUnicodeEscaper.outsideOf(32, 0x7f)
        );
    }
    
    public static final String escapeJava(final String input) {
        return ESCAPE_JAVA.translate(input);
    }
}
