package org.jboss.test.remoting.detection.jndi.deadlock3;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.multiplex.MultiplexInvokerConstants;
import org.jboss.remoting.transport.sslmultiplex.SSLMultiplexServerInvoker;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class Client implements NotificationListener, Runnable
{

   private static Logger logger;
   private String localHost = "";
   private String localPort;
   private static NetworkRegistry registry;


   private MBeanServer server;
   private ObjectName objConnector = null;

   private JNDIDetector jdet = null;
   private Connector initialConnector = null;
   private Connector connector;
   private org.jboss.remoting.Client client = null;
   private String jndiAddress = null;
   private int jndiPort = 2410;

   private void setLocalHost(String host)
   {
      jndiAddress = localHost = host;
   }


   private void setUp()
   {

      try
      {
         Thread s = new Thread(this);
         Runtime.getRuntime().addShutdownHook(s);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(1);
      }

      registry = NetworkRegistry.getInstance();

      try
      {
         server = MBeanServerFactory.createMBeanServer();
         server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      try
      {
         localPort = FindFreePort();
         InvokerLocator locator = new InvokerLocator("multiplex://" + localHost + ":" + localPort);

         //used until a server is found
         initialConnector = new Connector(locator.getLocatorURI());

         try
         {
            objConnector = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
            server.registerMBean(initialConnector, objConnector);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

         initialConnector.create();
         initialConnector.start();

      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(1);
      }


      jdet = new JNDIDetector();
      jdet.setPort(jndiPort);
      jdet.setHost(jndiAddress);
      jdet.setContextFactory("org.jnp.interfaces.NamingContextFactory");
      jdet.setURLPackage("org.jboss.naming:org.jnp.interfaces");
      jdet.setCleanDetectionNumber(2147483647);//avoids that the server is detected as gone
      //but it's no use if there are two or more servers


      try
      {
         server.registerMBean(jdet, new ObjectName("remoting:type=Detector,transport=jndi"));
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      try
      {
         jdet.start();
         registry.addNotificationListener(this, null, null);
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         System.exit(1);
      }

      //a third person tries to connect to the server
      try
      {
         Socket s = new Socket(localHost, 1001);
         s.close();
      }
      catch (UnknownHostException e)
      {
         e.printStackTrace();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }


   public void handleNotification(Notification notification, Object handback)
   {

      if (notification instanceof NetworkNotification)
      {
         NetworkNotification networkNotification = (NetworkNotification) notification;

         if (NetworkNotification.SERVER_ADDED.equals(networkNotification.getType()))
         { // notification is for new servers being added
            InvokerLocator[] locators = networkNotification.getLocator();
            for (int x = 0; x < locators.length; x++)
            {
               try
               {
                  //for this test make sure it's not a client
                  if (networkNotification.getLocator()[x].getPort() == 1001)
                  {
                     System.out.println("-+-Discovered server '" + locators[x].getLocatorURI() + "'-+-");
                     init(locators[x]);
                  }
               }
               catch (Exception ignored)
               {
               }
            }

         }
         else if (NetworkNotification.SERVER_REMOVED.equals(networkNotification.getType()))
         { // notification

            InvokerLocator[] locators = networkNotification.getLocator();
            for (int x = 0; x < locators.length; x++)
            {

               try
               {
                  //for this test make sure it's not a client
                  if (networkNotification.getLocator()[x].getPort() == 1001)
                  {
                     System.out.println("-!-Server '" + locators[x].getLocatorURI() + "' has gone-!-");
                  }
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }

            }
         }


      }

   }


   private static String FindFreePort()
   {

      ServerSocket socket = null;
      try
      {
         socket = new ServerSocket(0);
         return new Integer(socket.getLocalPort()).toString();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      finally
      {
         if (socket != null)
         {
            try
            {
               socket.close();
            }
            catch (IOException e)
            {
            }
         }
      }
      return null;

   }


   private synchronized void init(InvokerLocator locator)
   {
      localPort = FindFreePort();

      initServer(locator);//Test#1: commented out;   Test#2: not commented out

      try
      {

         Map configuration = new HashMap();
         configuration.put(MultiplexInvokerConstants.MULTIPLEX_BIND_HOST_KEY, localHost);
         configuration.put(MultiplexInvokerConstants.MULTIPLEX_BIND_PORT_KEY, localPort);


         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "certificate/clientKeyStore");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "testpw");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");

         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, "certificate/clientTrustStore");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "testpw");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");

         client = new org.jboss.remoting.Client(locator, "sample", configuration);
         client.connect();

         //initServer(locator);//Test#1: not commented out;   Test#2: commented out

         InvokerLocator clientLocator = new InvokerLocator("sslmultiplex://" + localHost + ":" + localPort);
         ClientCallbackHandler handler = new ClientCallbackHandler();
         client.addListener(handler, clientLocator);
         System.out.println("successful");
      }
      catch (Throwable e)
      {
         e.printStackTrace();

      }


   }


   private void initServer(InvokerLocator remoteLocator)
   {
      //building connector for found server
      Map configuration = new HashMap();
      String addr = remoteLocator.getHost();
      int port = remoteLocator.getPort();

      configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "certificate/clientKeyStore");
      configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "testpw");
      configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");

      configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, "certificate/clientTrustStore");
      configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "testpw");
      configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");

      try
      {
         connector = new Connector("sslmultiplex://" + localHost + ":" + localPort, configuration);

         connector.create();

         try
         {
            SSLMultiplexServerInvoker invoker = (SSLMultiplexServerInvoker) connector.getServerInvoker();
            ServerSocketFactory svrSocketFactory = SSL.createServerSocketFactory("testpw", "testpw", "certificate/clientKeyStore", "certificate/clientTrustStore");
            invoker.setServerSocketFactory(svrSocketFactory);
            invoker.setClientConnectAddress(addr);
            invoker.setClientConnectPort(port);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

         connector.start();


         if (initialConnector != null)
         {
            // don't need the initial connector any longer
            initialConnector.stop();
            try
            {
               server.unregisterMBean(objConnector);
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
            initialConnector = null;

         }
      }
      catch (Exception e)
      {
         e.printStackTrace();

      }

   }


   public void run()
   {

      try
      {
         jdet.stop();
         server.unregisterMBean(new ObjectName("remoting:type=Detector,transport=jndi"));
         registry.removeNotificationListener(this);
         registry = null;
         server.unregisterMBean(new ObjectName("remoting:type=NetworkRegistry"));

      }
      catch (Exception e)
      {
      }

      try
      {

         if (initialConnector != null)
         {
            initialConnector.stop();
            server.unregisterMBean(objConnector);
            initialConnector = null;
         }

         if (connector != null)
         {
            connector.stop();
            connector = null;
         }

         if (client != null)
         {
            client.disconnect();
            client = null;
         }

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }


   }


   public static void main(String[] args)
   {
      try
      {
         Client test = new Client();
         logger = Logger.getLogger(test.getClass());
         logger.setLevel((Level) Level.DEBUG);

         String host = InetAddress.getLocalHost().getHostAddress();

         test.setLocalHost(host);
         test.setUp();

         while (true)
         {
            Thread.sleep(1000);
         }

      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(1);
      }


   }


   public static class ClientCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {

      }

   }
}

