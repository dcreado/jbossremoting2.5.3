/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test.remoting.detection.jndi;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.multiplex.Multiplex;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author Michael Voss
 */
public class Client extends TestCase implements NotificationListener
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

   private int detectionCount = 0;
   private int detectionFailureCount = 0;

   private void setLocalHost(String host)
   {
      jndiAddress = localHost = host;
   }


   public void setUp() throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();

      setLocalHost(host);

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
      //jdet.setCleanDetectionNumber(2147483647);//avoids that the server is detected as gone
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
                  if (networkNotification.getLocator()[x].getPort() == 3001)
                  {
                     System.out.println("-+-Discovered server '" + locators[x].getLocatorURI() + "'-+-");
                     init(locators[x]);
                     detectionCount++;
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
                  if (networkNotification.getLocator()[x].getPort() == 3001)
                  {
                     System.out.println("-!-Server '" + locators[x].getLocatorURI() + "' has gone-!-");
                     detectionFailureCount++;
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

   public void testDetection() throws InterruptedException
   {
      Thread.sleep(60000);

      assertEquals(1, detectionCount);
      assertEquals(0, detectionFailureCount);
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

      initServer(locator);

      try
      {
         Map configuration = new HashMap();
         configuration.put(Multiplex.MULTIPLEX_BIND_HOST, localHost);
         configuration.put(Multiplex.MULTIPLEX_BIND_PORT, localPort);

         client = new org.jboss.remoting.Client(locator, "sample", configuration);
         client.connect();

      }
      catch (Exception e)
      {
         e.printStackTrace();

      }


   }


   private void initServer(InvokerLocator remoteLocator)
   {
      //building connector for found server
      Map configuration = new HashMap();
      String addr = remoteLocator.getHost();
      String port = new Integer(remoteLocator.getPort()).toString();
      configuration.put(Multiplex.MULTIPLEX_CONNECT_HOST, addr);
      configuration.put(Multiplex.MULTIPLEX_CONNECT_PORT, port);

      try
      {
         connector = new Connector("multiplex://" + localHost + ":" + localPort, configuration);

         connector.create();
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


   public void tearDown()
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


}

