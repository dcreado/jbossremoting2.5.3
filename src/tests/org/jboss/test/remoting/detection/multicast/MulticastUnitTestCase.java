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

package org.jboss.test.remoting.detection.multicast;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.multicast.MulticastDetector;
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
import java.util.ArrayList;

/**
 * Just tests that detector A sees detector B when B comes online then off.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MulticastUnitTestCase extends TestCase
{
   public MulticastUnitTestCase(String name)
   {
      super(name);
   }

   public void testNotifications() throws Exception
   {
      MulticastDetector detector1 = new MulticastDetector();
      MulticastDetector detector2 = new MulticastDetector();

      Connector connector1 = new Connector();
      Connector connector2 = new Connector();

      NetworkRegistry reg1 = setupServers(detector1, connector1);
      TestNotificationListener notif1 = new TestNotificationListener();
      reg1.addNotificationListener(notif1, null, null);

      NetworkRegistry reg2 = setupServers(detector2, connector2);
      TestNotificationListener notif2 = new TestNotificationListener();
      reg2.addNotificationListener(notif2, null, null);

      // Need to allow heartbeat so have detection
      Thread.currentThread().sleep(5000);

      //Should now have an entry for both of the registries
      int reg1Count = reg1.getServers().length;
      int reg2Count = reg2.getServers().length;

      // Actual junit test
      assertTrue(reg1Count == 1 && reg2Count == 1);

      if(reg1Count == 1 && reg2Count == 1)
      {
         System.out.println("PASSED - both registries have found the detectors.");
      }
      else
      {
         System.out.println("FAILED - registries not populated with remote detectors.");
      }

      assertTrue(((String)notif1.notifLog.get(0)).startsWith("ADDED"));
      assertTrue(((String)notif1.notifLog.get(1)).startsWith("ADDED"));
      System.out.println("Notifications from Registry #1--->" + notif1.notifLog);
      assertTrue(((String)notif2.notifLog.get(0)).startsWith("ADDED"));
      assertTrue(((String)notif2.notifLog.get(1)).startsWith("ADDED"));
      System.out.println("Notifications from Registry #2--->" + notif2.notifLog);

      // stop the 2nd detector, so see if 1st one detects it is missing
      connector1.stop();
      connector1.destroy();
      connector1 = null;
      connector2.stop();
      connector2.destroy();
      connector2 = null;
      detector1.stop();

      // sleep for a few seconds so the 1st detector can discover 2nd one down
      Thread.sleep(60000);

      // 1st one should be empty
      reg1Count = reg2.getServers().length;

      // Actual junit test
      assertTrue(reg1Count == 0);

      if(reg1Count == 0)
      {
         System.out.println("PASSED - 2nd detector stopped and no longer in registry.");
      }
      else
      {
         System.out.println("FAILED - 2nd detector stopped but still in registry.");
      }

      assertTrue(((String)notif2.notifLog.get(0)).startsWith("ADDED"));
      assertTrue(((String)notif2.notifLog.get(1)).startsWith("ADDED"));
      assertTrue(((String)notif2.notifLog.get(2)).startsWith("REMOVED"));
      assertTrue(((String)notif2.notifLog.get(3)).startsWith("REMOVED"));
      System.out.println("Notifications from Registry #2-->" + notif2.notifLog);

      // cleanup
      detector2.stop();
      //connector2.stop();
      //connector2.destroy();
   }

   public void testDetectors() throws Exception
   {
      MulticastDetector detector1 = new MulticastDetector();
      MulticastDetector detector2 = new MulticastDetector();

      Connector connector1 = new Connector();
      Connector connector2 = new Connector();

      NetworkRegistry reg1 = setupServers(detector1, connector1);
      NetworkRegistry reg2 = setupServers(detector2, connector2);

      // Need to allow heartbeat so have detection
      Thread.currentThread().sleep(2000);

      //Should now have an entry for both of the registries
      int reg1Count = reg1.getServers().length;
      int reg2Count = reg2.getServers().length;

      // Actual junit test
      assertTrue(reg1Count == 1 && reg2Count == 1);

      if(reg1Count == 1 && reg2Count == 1)
      {
         System.out.println("PASSED - both registries have found the detectors.");
      }
      else
      {
         System.out.println("FAILED - registries not populated with remote detectors.");
      }

      // stop the 2nd detector, so see if 1st one detects it is missing
      connector1.stop();
      connector1.destroy();
      connector1 = null;
      connector2.stop();
      connector2.destroy();
      connector2 = null;
      detector1.stop();

      // sleep for a few seconds so the 1st detector can discover 2nd one down
      Thread.currentThread().sleep(60000);

      // 1st one should be empty
      reg1Count = reg2.getServers().length;

      // Actual junit test
      assertTrue(reg1Count == 0);

      if(reg1Count == 0)
      {
         System.out.println("PASSED - 2nd detector stopped and no longer in registry.");
      }
      else
      {
         System.out.println("FAILED - 2nd detector stopped but still in registry.");
      }

      // cleanup
      detector2.stop();
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
         buf.append("  <handler subsystem=\"mock\">org.jboss.test.remoting.transport.mock.MockServerInvocationHandler</handler>\n");
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

   private static class TestNetworkRegistry extends NetworkRegistry
   {
      public static NetworkRegistry createNetworkRegistry()
      {
         return new TestNetworkRegistry();
      }
   }

   private static class TestNotificationListener implements NotificationListener
   {
      public ArrayList notifLog = new ArrayList();

      public void handleNotification( Notification notification, Object o )
      {
         System.out.println("TestNotificationListner.handleNotification() called.");

         if ( notification instanceof NetworkNotification )
         {
            NetworkNotification networkNotification = (NetworkNotification) notification;

            if ( NetworkNotification.SERVER_ADDED.equals( networkNotification.getType() ) )
            {
               System.out.println("SERVER_ADDED notification.");
               for (int i = 0; i < networkNotification.getLocator().length; i++)
               {
                  System.out.println("ADDED: " + networkNotification.getLocator()[i]);
                  notifLog.add("ADDED: " + networkNotification.getLocator()[i]);
               }
            }
            else if ( NetworkNotification.SERVER_REMOVED.equals( networkNotification.getType() ) )
            {
               System.out.println("SERVER_REMOVED notification.");
               for (int i = 0; i < networkNotification.getLocator().length; i++)
               {
                  System.out.println("REMOVED: " + networkNotification.getLocator()[i]);
                  notifLog.add("REMOVED: " + networkNotification.getLocator()[i]);
               }
            }
            else if ( NetworkNotification.SERVER_UPDATED.equals( networkNotification.getType() ) )
            {
               System.out.println("SERVER_UPDATED notification.");
               for (int i = 0; i < networkNotification.getLocator().length; i++)
               {
                  System.out.println("UPDATED: " + networkNotification.getLocator()[i]);
                  notifLog.add("UPDATED: " + networkNotification.getLocator()[i]);
               }
            }
         }
      }
   }
}
