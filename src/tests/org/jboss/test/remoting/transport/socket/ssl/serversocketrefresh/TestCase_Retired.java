package org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh;
import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;
import org.jboss.logging.XLevel;

/**
 * JBREM-427 load a new keystore at runtime<br>
 * server is refreshing its serversocket after a new ServerSocketFactory was set<br>
 * second client connection attempt fails because client is not accepted by the new truststore
 * @author Michael Voss
 *
 */
public class TestCase_Retired extends TestDriver{
	
	public void declareTestClasses()
	   {
	      addTestClasses(TestClient.class.getName(),
	                     1,
	                     TestServer.class.getName());

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
