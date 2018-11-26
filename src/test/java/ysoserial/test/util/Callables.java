package ysoserial.test.util;

import java.util.concurrent.Callable;

public class Callables {
    public static interface BeforeAfterCallback {
        public void before();
        public void after();
    }

    public static class Wrapper<T> implements Callable<T> {
        private final Callable<T> callable;
        private final BeforeAfterCallback callback;

        public Wrapper(Callable<T> callable, BeforeAfterCallback callback) {
            this.callable = callable;
            this.callback = callback;
        }

        @Override
        public T call() throws Exception {
            try {
                callback.before();
                return callable.call();
            } finally {
                callback.after();
            }
        }
    }

    public static <T> Callable<T> wrap(Callable<T> callable, BeforeAfterCallback callback) {
        return new Wrapper<T>(callable, callback);
    }
}
