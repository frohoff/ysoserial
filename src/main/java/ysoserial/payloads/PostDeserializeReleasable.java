package ysoserial.payloads;


public interface PostDeserializeReleasable<T> extends ObjectPayload<T> {
    void postDeserializeRelease(T obj) throws Exception;
}
