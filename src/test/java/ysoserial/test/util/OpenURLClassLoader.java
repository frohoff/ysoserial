package ysoserial.test.util;

import java.net.URL;
import java.net.URLClassLoader;

public class OpenURLClassLoader extends URLClassLoader {
    public OpenURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> defineNewClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }
}
