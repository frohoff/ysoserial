package ysoserial.payloads;


import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class ParameterizedObjectPayload<T> implements ObjectPayload<T> {
    /**
     * Displays specific help
     *
     * @return help to be displayed in console
     */
    abstract public String getHelp();

    /***
     * New parameterized chain
     * @param args arguments to pass to the gadget
     * @return generated gadget chain
     * @throws Exception if error occurs
     */
    abstract public T getObject(String[] args) throws Exception;

    /**
     * Method to keep backward compatibility with ObjectPayload
     * mainly used to call java.lang.Runtime.exec(String)
     */
    @Override
    public T getObject(String command) throws Exception {
        final StringTokenizer tokenizer = new StringTokenizer(command);
        final List<String> commandTokenized = new LinkedList<String>();
        while (tokenizer.hasMoreTokens()) {
            commandTokenized.add(tokenizer.nextToken());
        }
        final String[] commandTokenizedArray = commandTokenized.toArray(new String[0]);
        return this.getObject(commandTokenizedArray);
    }
}
