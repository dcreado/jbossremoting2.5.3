package org.jboss.test.remoting.detection.multicast.deadlock;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for JBREM-553
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MulticastDetectorServer extends ServerTestCase
{
   private MulticastDetector detector;
   private Connector connector;
   private NetworkRegistry registry;
   private Map config = new HashMap();

   public void setUp() throws Exception
   {
      detector = new MulticastDetector();

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = null;
      
      try
      {
          server = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                 return MBeanServerFactory.createMBeanServer();
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (Exception) e.getCause();
      }

      registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

      int port = TestUtil.getRandomPort();
      System.out.println("port = " + port);

      String host = InetAddress.getLocalHost().getHostAddress();
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      InvokerLocator locator = new InvokerLocator("sslsocket://" + bindAddr + ":" + port);

      System.out.println("Starting remoting server with locator uri of: " + locator.getLocatorURI());

      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("ssl/.truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      String keyStoreFilePath = this.getClass().getResource("ssl/.keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      config.put(SSLSocketBuilder.REMOTING_CLIENT_AUTH_MODE, SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

      connector = new Connector(config);
      connector.setInvokerLocator(locator.getLocatorURI());

      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
      server.registerMBean(connector, obj);
      connector.create();

      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("mock", handler);
      connector.start();

      //Need to set new domain for identity
      server.registerMBean(detector, new ObjectName("remoting:type=MulticastDetector"));

      detector.start();

   }

   public void tearDown() throws Exception
   {
      if (detector != null)
      {
         detector.stop();
      }
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);

      try
      {
         MulticastDetectorServer test = new MulticastDetectorServer();
         test.setUp();
         Thread.currentThread().sleep(10000);
         test.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }

   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object obj = invocation.getParameter();
         if (obj instanceof String)
         {
            String locator = (String) obj;
            ServerClient client = new ServerClient(locator);
            Thread t = new Thread(client, "server_client_thread");
            //t.setDaemon(false);
            t.setDaemon(true);
            t.start();
         }


         return "foobar";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }

   public class ServerClient implements Runnable
   {
      private String locatorUrl = null;

      public ServerClient(String locator)
      {
         this.locatorUrl = locator;
      }

      public void run()
      {

         try
         {
            Client remotingClient = null;
            for (int x = 0; x < 2; x++)
            {
               remotingClient = new Client(new InvokerLocator(locatorUrl), config);
               remotingClient.connect();
               Object ret = remotingClient.invoke("bar");
               System.out.println("client returned " + ret);
               Thread.currentThread().sleep(3000);
            }
            remotingClient.disconnect();
            System.out.println("server client disconnected.");

         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
         catch (Throwable throwable)
         {
            throwable.printStackTrace();
         }

      }
   }


}
