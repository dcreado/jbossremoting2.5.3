package org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Michael Voss
 *
 */
public class TestServer extends ServerTestCase{
   private static Logger log = Logger.getLogger("TestServer");
   protected String localHost="127.0.0.1";
   protected String keyStorePath="src/tests/org/jboss/test/remoting/transport/socket/ssl/serversocketrefresh/certificate/serverKeyStore";
   protected String trustStorePath="src/tests/org/jboss/test/remoting/transport/socket/ssl/serversocketrefresh/certificate/serverTrustStore";
   protected String keyStorePassword="testpw";
   protected String trustStorePassword="testpw";
   protected String port="2001";
   protected Connector connector;
   protected MBeanServer server;
   protected static boolean invocationDone=false;
	
	public void setUp(){	
		log.info("entering setUp");
		// starting connector
		try{
			//make it SSL-ready for sending callbacks (server acts as a client)
			Map configuration = new HashMap();
		
         keyStorePath = this.getClass().getResource("certificate/serverKeyStore").getFile();
         trustStorePath = this.getClass().getResource("certificate/serverTrustStore").getFile();

         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
			configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStorePath);
			configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, keyStorePassword);
			configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
			
			configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
			configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStorePath);
			configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, trustStorePassword);
			configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");
			configuration.put("reuseAddress", "true");
		    connector = new Connector(configuration);
			
		    InvokerLocator locator = new InvokerLocator(getTransport() + "://"+localHost+":"+port);
		    connector.setInvokerLocator(locator.getLocatorURI());
		    server=MBeanServerFactory.createMBeanServer();
		    server.registerMBean(connector,  new ObjectName("jboss.remoting:type=Connector"));
			connector.create();
			
			//make it SSL-ready for clients that want to build up connection:			
			try{
				// create server socket factory
				ServerSocketFactory svrSocketFactory = createServerSocketFactory(keyStorePassword,trustStorePassword,keyStorePath,trustStorePath);
				
				// notice that the invoker has to be explicitly cast to the
				// SocketServerInvoker type								
				SocketServerInvoker socketSvrInvoker = (SocketServerInvoker) connector.getServerInvoker();
				socketSvrInvoker.setServerSocketFactory(svrSocketFactory);
			}
			catch(Exception e){
				e.printStackTrace();
			}			
			
			connector.addInvocationHandler("Sample",new Sample());
			connector.start();
			log.info("leaving setUp()");
		}
		catch(Exception e){			
			e.printStackTrace();
		}		
								    		    
	}
	public void tearDown() throws Exception
	   {
       log.info("entering tearDown()");
		Thread.sleep(10000);
		server.unregisterMBean( new ObjectName("jboss.remoting:type=Connector"));
		connector.removeInvocationHandler("Sample");
		connector.stop();
        log.info("stopped Connector");
	   }
	

	public void test() throws Exception
	{
       log.info("entering test()");
	   for (int i = 0; i < 5; i++)
	   {
	      if (invocationDone) break;
	      Thread.sleep(2000);
	   }
	   assertTrue(invocationDone);
       log.info("invocation done");
	   String keyStorePath2 = this.getClass().getResource("certificate/serverTrustStore2").getFile();
	   ((SocketServerInvoker) connector.getServerInvoker()).setNewServerSocketFactory(createServerSocketFactory(keyStorePassword,trustStorePassword,keyStorePath,keyStorePath2));
	   Thread.sleep(250000);//let client build up connection a second time
       log.info("leaving test()");
	}

   public static void main(String[] args)
   {
      TestServer server = new TestServer();
      server.setUp();
      try
      {
         server.test();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

    protected String getTransport()
    {
       return "sslsocket";
    }

   /**
	 * @author Michael Voss
	 *
	 */
	public static class Sample implements ServerInvocationHandler
	   {
	    
	      /**
	       * called by the remoting server to handle the invocation from client.
	       *
	       * @param invocation
	       * @return Object
	       * @throws Throwable
	       */
	      public Object invoke(InvocationRequest invocation) throws Throwable
	      {
	    	  invocationDone=true;
	    	  System.err.println("client invoked: "+invocation.getParameter());
	    	  System.err.println("answering: succeess");
	    	  return "success";
	      }

	      /**
	       * Adds a callback handler that will listen for callbacks from
	       * the server invoker handler.
	       *
	       * @param callbackHandler
	       */
	      public void addListener(InvokerCallbackHandler callbackHandler)
	      {
	         
	      }

	      /**
	       * Removes the callback handler that was listening for callbacks
	       * from the server invoker handler.
	       *
	       * @param callbackHandler
	       */
	      public void removeListener(InvokerCallbackHandler callbackHandler)
	      {
	         
	      }


	      /**
	       * set the mbean server that the handler can reference
	       *
	       * @param server
	       */
	      public void setMBeanServer(MBeanServer server)
	      {
	         // NO OP as do not need reference to MBeanServer for this handler
	      }

	      /**
	       * set the invoker that owns this handler
	       *
	       * @param invoker
	       */
	      public void setInvoker(ServerInvoker invoker)
	      {
	         // NO OP as do not need reference back to the server invoker
	      }
	   }
	
	/**
	 * returns a SSLServerSocketFactory that requires a client certificate to build up SSL connection
	 * @param keyStorePassword
	 * @param trustStorePassword
	 * @param keyStorePath
	 * @param trustStorePath
	 * @return SSLServerSocketFactory
	 * @throws Exception
	 */
	public static SSLServerSocketFactory createServerSocketFactory(String keyStorePassword, String trustStorePassword, String keyStorePath, String trustStorePath) throws Exception
	{				 
		return new ClientAuthSocketFactory(createNoAuthServerSocketFactory(keyStorePassword,trustStorePassword,keyStorePath,trustStorePath));
	}
	
	/**
	 * returns a SSLServerSocketFactory
	 * @param keyStorePassword
	 * @param trustStorePassword
	 * @param keyStorePath
	 * @param trustStorePath
	 * @return SSLServerSocketFactory
	 * @throws Exception
	 */
	public static SSLServerSocketFactory createNoAuthServerSocketFactory(String keyStorePassword, String trustStorePassword, String keyStorePath, String trustStorePath) throws Exception
	{		
		FileInputStream stream=null;
		try {
			// create an SSLContext
			SSLContext context = null;

			context = SSLContext.getInstance("TLS");

			// define password
			char[] keyPassphrase = keyStorePassword.toCharArray();
			char[] trustPassphrase = trustStorePassword.toCharArray();
			
			// load the server key store
			KeyStore server_keystore = KeyStore.getInstance("JKS");
			stream = new FileInputStream(keyStorePath);
			server_keystore.load(stream, keyPassphrase);
			stream.close();
			
			// load the server trust store
			KeyStore server_truststore = KeyStore.getInstance("JKS");
			stream = new FileInputStream(trustStorePath);
			server_truststore.load(stream, trustPassphrase);
			stream.close();
			
			// initialize a KeyManagerFactory with the KeyStore
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(server_keystore, keyPassphrase);			
			// KeyManagers from the KeyManagerFactory
			KeyManager[] keyManagers = kmf.getKeyManagers();

			// initialize a TrustManagerFactory with the TrustStore
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(server_truststore);
			// TrustManagers from the TrustManagerFactory
			TrustManager[] trustManagers = tmf.getTrustManagers();

			// initialize context with Keystore and Truststore information
			context.init(keyManagers, trustManagers, null);

			// get ServerSocketFactory from context
			return context.getServerSocketFactory();
			
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				stream.close();
			} catch (Exception ioe) {
			}
		}
	}
	
	/**
	 * overrides createServerSocket methods from class SSLServerSocketFactory<br>
	 * sets NeedClientAuth true, so server asks for a client certificate in the SSL handshake
	 * @author <a href="mailto:michael.voss@hp.com">Michael Voss</a>
	 *
	 */
	public static class ClientAuthSocketFactory extends SSLServerSocketFactory{
		SSLServerSocketFactory serverSocketFactory;

	    /**
	     * @param serverSocketFactory
	     */
	    public ClientAuthSocketFactory(SSLServerSocketFactory serverSocketFactory)
	    {
	       this.serverSocketFactory = serverSocketFactory;
	    }

	    public ServerSocket createServerSocket() throws IOException
	    {
	       SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket();
	       ss.setNeedClientAuth(true);
	       return ss;
	    }

	    public ServerSocket createServerSocket(int arg0) throws IOException
	    {
	       SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0);
	       ss.setNeedClientAuth(true);
	       return ss;
	    }

	    public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
	    {
	       SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0, arg1);
	       ss.setNeedClientAuth(true);
	       return ss;
	    }

	    public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
	    {
	       SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0, arg1, arg2);
	       ss.setNeedClientAuth(true);
	       return ss;
	      
	    }

	    public boolean equals(Object obj)
	    {
	       return serverSocketFactory.equals(obj);
	    }

	    public String[] getDefaultCipherSuites()
	    {
	       return serverSocketFactory.getDefaultCipherSuites();
	    }

	    public String[] getSupportedCipherSuites()
	    {
	       return serverSocketFactory.getSupportedCipherSuites();
	    }

	    public int hashCode()
	    {
	       return serverSocketFactory.hashCode();
	    }

	    public String toString()
	    {
	       return serverSocketFactory.toString();
	    }
	}
}
