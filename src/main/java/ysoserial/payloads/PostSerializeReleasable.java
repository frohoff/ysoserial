package ysoserial.payloads;

public interface PostSerializeReleasable<T> extends ObjectPayload<T> {
    void postSerializeRelease(T obj ) throws Exception;
}
