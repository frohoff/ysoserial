package ysoserial.interfaces;




public interface ObjectPayload <T> {

    /*
	 * return armed payload object to be serialized that will execute specified
	 * command on deserialization
	 */
	/**
	 * @deprecated Use {@link #getObject()} instead
	 */
	public T getObject ( String command ) throws Exception;

	/*
     * return armed payload object to be serialized that will execute specified
     * command on deserialization
     */
    public T getObject ( ) throws Exception;
}
