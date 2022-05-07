package ysoserial.secmgr;

import java.util.concurrent.Callable;

public class SecurityManagers {
    public static <T> Callable<T> wrapped(final Callable<T> callable, final SecurityManager sm) throws Exception {
        final SecurityManager orig = System.getSecurityManager(); // save sm
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                System.setSecurityManager(sm);
                try {
                    return callable.call();
                } finally {
                    System.setSecurityManager(orig); // restore sm
                }

            }
        };
    }
}
