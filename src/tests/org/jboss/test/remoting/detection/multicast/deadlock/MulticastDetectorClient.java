package org.jboss.test.remoting.detection.multicast.deadlock;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for JBREM-553
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MulticastDetectorClient extends TestCase
{
   private MulticastDetector detector;
   private NetworkRegistry registry;
   private Connector connector;

   private Client remotingClient = null;

   public void setUp() throws Exception
   {
      detector = new MulticastDetector();

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

      //Need to set new domain for identity
      server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));
   }

   public void testDetection() throws Exception
   {
      detector.start();
      long start = System.currentTimeMillis();
      NetworkInstance[] instances = detector.forceDetection();
      long end = System.currentTimeMillis();

      System.out.println("instance = " + instances);
      System.out.println("force detection took " + (end - start) + " milliseconds.");

      String host = InetAddress.getLocalHost().getHostAddress();
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      
//      assertEquals(1, instances.length);

      // now create a client
      InvokerLocator serverLocator = null;
      for (int i = 0; i < instances.length; i++)
      {     
         NetworkInstance ni = instances[i];
         InvokerLocator[] locator = ni.getLocators();
         for (int j = 0; j < locator.length; j++)
         {
            if (locator[j].getHost().equals(bindAddr))
            {
               serverLocator = locator[j];
               break;
            }
         }
      }

      System.out.println("client connecting to " + serverLocator);
      Map config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("ssl/.truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      String keyStoreFilePath = this.getClass().getResource("ssl/.keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      config.put(SSLSocketBuilder.REMOTING_CLIENT_AUTH_MODE, SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

      remotingClient = new Client(serverLocator, config);
      remotingClient.connect();

      String invokerLocatorurl = "sslsocket://" + bindAddr + ":8700";
      connector = new Connector(invokerLocatorurl, config);
      connector.create();

      connector.addInvocationHandler("test", new LocalHandler());
      connector.start();

      try
      {
         Object ret = remotingClient.invoke(invokerLocatorurl);
         System.out.println("return from calling server is " + ret);
      }
      catch (Throwable throwable)
      {
         throwable.printStackTrace();
         throw new Exception(throwable.getMessage());
      }

//      Thread.currentThread().sleep(20000);
//
//      System.out.println("Disconnecting.");
//
//      remotingClient.disconnect();
//
//      System.out.println("Disconnected.");
   }

   public void disconnect() throws InterruptedException
   {
      Thread.currentThread().sleep(30000);

      System.out.println("Disconnecting.");

      remotingClient.disconnect();

      System.out.println("Disconnected.");

      remotingClient = null;

   }

   public void tearDown() throws Exception
   {
      if (remotingClient != null)
      {
         remotingClient.disconnect();
      }
      if (detector != null)
      {
         detector.stop();
      }
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      MulticastDetectorClient client = new MulticastDetectorClient();
      try
      {
         client.setUp();
         client.testDetection();
         client.disconnect();
         Thread.sleep(5000);
         System.out.println("done testing.");
      }
      catch(Throwable t)
      {
         t.printStackTrace();
      }
      finally
      {
         try
         {
            client.tearDown();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }


   public class LocalHandler implements ServerInvocationHandler
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
         return "foo";
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


}
