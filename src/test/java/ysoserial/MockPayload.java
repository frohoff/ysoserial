package ysoserial;

import java.io.Serializable;

import ysoserial.payloads.ObjectPayload;

public class MockPayload implements ObjectPayload {
	private final Serializable obj;
	
	public MockPayload(final Serializable obj) {
		this.obj = obj;
	}

	public Object getObject(final String command) throws Exception {
		return obj;
	}
}