package org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Voss
 *
 */
public class TestClient extends TestCase{
	private static Logger log = Logger.getLogger(TestClient.class);
	private String keyStorePath="src/tests/org/jboss/test/remoting/transport/socket/ssl/serversocketrefresh/certificate/clientKeyStore";
	private String trustStorePath="src/tests/org/jboss/test/remoting/transport/socket/ssl/serversocketrefresh/certificate/clientTrustStore";
	private String keyStorePassword="testpw";
	private String trustStorePassword="testpw";
	InvokerLocator locator;
	Client client;
	Map configuration;
	
	public void setUp() throws Exception{
       log.info("entering setUp()");
		locator=new InvokerLocator(getTransport() + "://localHost:2001");
		
		//the client side connection endpoint
		configuration = new HashMap();

      //String keyStorePath = this.getClass().getResource("certificate/serverKeyStore").getFile();
      //String trustStorePath = this.getClass().getResource("certificate/serverTrustStore").getFile();
		String keyStorePath = this.getClass().getResource("certificate/clientKeyStore").getFile();
	    String trustStorePath = this.getClass().getResource("certificate/clientTrustStore").getFile();

        configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
		configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStorePath);
		configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, keyStorePassword);
		configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
		
		configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
		configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStorePath);
		configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, trustStorePassword);
		configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");
		
		client=new Client(locator,"Sample",configuration);				
		
		client.connect();
        log.info("leaving setUp()");
		
	}
	
	public void tearDown() throws Exception{
       log.info("entering tearDown()");
		Thread.sleep(10000);//let the server fetch his new truststore and refresh serversocket
		log.info("finished first sleep in tearDown()");
        client.disconnect();
        log.info("disconnected client in tearDown()");
		Thread.sleep(350000);
        log.info("finished second sleep in tearDown()");

		try {
            setUp();//secondPass -> client is not accepted by new truststore
			test();//secondPass -> client is not accepted by new truststore
		} catch (Throwable e) {
           log.error("expected Exception", e);
			client.disconnect();
            log.info("SUCCESS");
			return;
		}
		client.disconnect();
		throw new Exception("should not reach this point");//because client should not be accepted by new server truststore
		
	}
	
	/**
	 * @throws Throwable
	 */
	public void test() throws Throwable{
		log.info("Invoking server with 'something'");
		log.info("Server answer is: "+client.invoke("something"));
	}
	
	protected String getTransport()
	{
	   return "sslsocket";
	}
	
}
