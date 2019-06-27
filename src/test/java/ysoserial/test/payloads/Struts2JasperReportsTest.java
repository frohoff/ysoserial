package ysoserial.test.payloads;

import java.util.HashMap;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.providers.XWorkConfigurationProvider;
import com.opensymphony.xwork2.inject.Container;

import ysoserial.Deserializer;
import ysoserial.test.CustomDeserializer;

public class Struts2JasperReportsTest extends CommandExecTest implements CustomDeserializer {

	@Override
	public Class<?> getCustomDeserializer() {
		return StrutsJasperReportsDeserializer.class;
	}
	
    /**
     * need to use a custom deserializer so that the action context gets set in the isolated class
     *
     * @author sciccone
     *
     */
    public static final class StrutsJasperReportsDeserializer extends Deserializer {
        
		public StrutsJasperReportsDeserializer(byte[] bytes) {
			super(bytes);
		}
    	
        @Override
        public Object call () throws Exception {
            ConfigurationManager configurationManager = new ConfigurationManager(Container.DEFAULT_NAME);
            configurationManager.addContainerProvider(new XWorkConfigurationProvider());
            Configuration config = configurationManager.getConfiguration();
            Container container = config.getContainer();
            
        	HashMap<String, Object> context = new HashMap<String, Object>();
        	context.put(ActionContext.CONTAINER, container);
        	ActionContext.setContext(new ActionContext(context));
        	return super.call();
        }
    }

}
