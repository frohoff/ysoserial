package ysoserial.payloads;

public interface ObjectPayload<T> {
	/*
	 * return armed payload object to be serialized that will execute specified 
	 * command on deserialization
	 */
	public T getObjectExec(String command) throws Exception;
	public T getObjectSleep(String command) throws Exception;
}
