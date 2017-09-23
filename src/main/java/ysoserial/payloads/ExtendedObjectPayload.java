package ysoserial.payloads;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class ExtendedObjectPayload<T> implements ObjectPayload<T> {
	abstract public T getObject(String[] command) throws Exception;
	
	/**
	 * Method to keep backward compatibility with ObjectPayload
	 * using StringTokenizer used in java.lang.Runtime.exec(String)
	 */
	@Override
	public T getObject(String command) throws Exception {
		final StringTokenizer tokenizer = new StringTokenizer(command);
		final List<String> commandTokenized = new LinkedList<String>();
		while (tokenizer.hasMoreTokens()) {
			commandTokenized.add(tokenizer.nextToken());
		}
		final String[] commandTokenizedArray= commandTokenized.toArray(new String[0]);
		return this.getObject(commandTokenizedArray);
	}
}
