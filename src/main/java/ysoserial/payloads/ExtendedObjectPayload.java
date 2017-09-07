package ysoserial.payloads;

public abstract class ExtendedObjectPayload<T> implements ObjectPayload<T> {
	abstract public T getObject(String[] command) throws Exception;
	
	@Override
	public T getObject(String command) throws Exception {
		// FIXME Use tokenizer
		return this.getObject(new String[] { command });
	}
}
