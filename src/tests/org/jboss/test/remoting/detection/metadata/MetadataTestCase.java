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

package org.jboss.test.remoting.detection.metadata;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.w3c.dom.Document;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Just tests that detector A sees detector B when B comes online then off.
 * Also checks to make sure the detection message contains the proper data.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MetadataTestCase extends TestCase implements NotificationListener
{
   private static int secret = Math.abs(new Random().nextInt(2000));

   private HashSet subSystems = new HashSet();

   public MetadataTestCase(String name)
   {
      super(name);
   }

   public void testDetectors() throws Exception
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      MulticastDetector detector1 = new MulticastDetector();
      MulticastDetector detector2 = new MulticastDetector();

      Connector connector1 = new Connector();
      Connector connector2 = new Connector();
      
      try
      {
         NetworkRegistry reg1 = setupServers(detector1, connector1);
         // need to register with the mbean server for notifications
         List mbeanServers = (List) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return MBeanServerFactory.findMBeanServer(null);
            }
         });

         MBeanServer mbeanSvr = (MBeanServer) mbeanServers.get(0);
         mbeanSvr.addNotificationListener(new ObjectName("remoting:type=NetworkRegistry"),
                                          this, null, null);

         NetworkRegistry reg2 = setupServers(detector2, connector2);

         // Need to allow heartbeat so have detection
         Thread.currentThread().sleep(2000);

         //Should now have an entry for both of the registries
         int reg1Count = reg1.getServers().length;
         int reg2Count = reg2.getServers().length;
         System.out.println("registry 1: " + reg1Count);
         System.out.println("registry 2: " + reg2Count);
         
         if(reg1Count >= 1 && reg2Count >= 1)
         {
            System.out.println("PASSED - both registries have found detectors.");
         }
         else
         {
            System.out.println("FAILED - registries not populated with remote detectors.");
         }

         // Actual junit test
         assertTrue(reg1Count >= 1 && reg2Count >= 1);
         
         // Verify the Connectors created by this test have been detected.
         checkForConnector(reg1);
         checkForConnector(reg2);

         // now check to make sure got the subsystem as expected
         assertTrue(subSystems.contains("MOCK"));
      }
      finally
      {
         // stop the 2nd detector, so see if 1st one detects it is missing
         if (connector1 != null) 
         {
            connector1.stop();
            connector1.destroy();
            connector1 = null;
         }
         if (connector2 != null)
         {
            connector2.stop();
            connector2.destroy();
            connector2 = null;
         }
         if (detector1 != null) detector1.stop();
         if (detector2 != null) detector2.stop();
      }
      //connector2.stop();
      //connector2.destroy();
   }


   private synchronized NetworkRegistry setupServers(MulticastDetector detector, Connector connector)
   {
      NetworkRegistry registry = null;
      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      try
      {
         MBeanServer server = MBeanServerFactory.createMBeanServer();

         //registry = NetworkRegistry.getInstance();
         registry = TestNetworkRegistry.createNetworkRegistry();
         server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

         //int port = Math.abs(new Random().nextInt(2000));
         int port = TestUtil.getRandomPort();
         System.out.println("port = " + port);
         
         String host = InetAddress.getLocalHost().getHostAddress();
         String bindAddr = System.getProperty("jrunit.bind_addr", host);
         InvokerLocator locator = new InvokerLocator("socket://" + bindAddr + ":" + port);
         
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<handlers>\n");
         buf.append("  <handler subsystem=\"mock\">" + TestInvocationHandler.class.getName() + "</handler>\n");
         buf.append("</handlers>\n");
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
         connector.setInvokerLocator(locator.getLocatorURI());
         connector.setConfiguration(xml.getDocumentElement());
         ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
         server.registerMBean(connector, obj);
         //connector.create();
         connector.start();

         //Need to set new domain for identity
         server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));

         // set config info for detector and start it.
         detector.start();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

      return registry;
   }

   public void handleNotification(Notification notification, Object o)
   {
      System.out.println("Received notification: " + notification);
      if(notification instanceof NetworkNotification)
      {
         NetworkNotification netNot = (NetworkNotification) notification;
         ServerInvokerMetadata[] serverMetadata = netNot.getServerInvokers();
         for (int i = 0; i < serverMetadata.length; i++)
         {
            String[] ss = serverMetadata[i].getSubSystems();
            for (int j = 0; j < ss.length; j++)
            {
               subSystems.add(ss[j]);
            }
         }
      }
   }

   private boolean checkForConnector(NetworkRegistry registry)
   {
      boolean found = false;
      NetworkInstance[] servers1 = registry.getServers();
      for (int i = 0; i < servers1.length; i++)
      {
         InvokerLocator[] locators = servers1[i].getLocators();
         for (int j = 0; j < locators.length; j++)
         {
            try
            {
               Client client = new Client(locators[j]);
               client.connect();
               if (secret == ((Integer) client.invoke("abc")).intValue())
               {
                  found = true;
                  System.out.println("FOUND: " + locators[j]);
                  break;
               }
            }
            catch (Throwable t)
            {
               continue;
            }
         }
      }
      
      return found;
   }
   
   private static class TestNetworkRegistry extends NetworkRegistry
   {
      public static NetworkRegistry createNetworkRegistry()
      {
         return new TestNetworkRegistry();
      }
   }

   public static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return new Integer(secret);
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}
