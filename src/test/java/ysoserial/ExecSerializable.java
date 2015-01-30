package ysoserial;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public class ExecSerializable implements Serializable {
	private final String cmd;	
	public ExecSerializable(String cmd) { this.cmd = cmd; }

	private void readObject(final ObjectInputStream ois) {
		try {
			Runtime.getRuntime().exec("hostname");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}		
}