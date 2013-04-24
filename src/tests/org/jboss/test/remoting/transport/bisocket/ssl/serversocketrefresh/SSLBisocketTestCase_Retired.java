package org.jboss.test.remoting.transport.bisocket.ssl.serversocketrefresh;
import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;
import org.jboss.logging.XLevel;

/**
 * JBREM-427 load a new keystore at runtime<br>
 * server is refreshing its serversocket after a new ServerSocketFactory was set<br>
 * second client connection attempt fails because client is not accepted by the new truststore
 * @author Michael Voss
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 *
 */
public class SSLBisocketTestCase_Retired extends TestDriver{
	
	public void declareTestClasses()
	   {
	      addTestClasses(SSLBisocketTestClient.class.getName(),
	                     1,
	                     SSLBisocketTestServer.class.getName());

	   }
    
    protected Level getTestLogLevel()
    {
       return XLevel.TRACE;
    }
    
    protected long getResultsTimeout()
    {
       return 480000;
    }
    
    protected long getRunTestTimeout()
    {
       return 480000;
    }
}
