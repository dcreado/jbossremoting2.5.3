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
package org.jboss.test.remoting.detection.registry;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class NetworkRegistryTestCase extends TestCase implements NotificationListener
{
   private static Logger log = Logger.getLogger(NetworkRegistryTestCase.class);
   private String subSystem = null;
   private int numOfAdded = 0;
   private int numOfUpdated = 0;
   private InvokerLocator locator1;
   private InvokerLocator locator2;

   public void testRegistration() throws Exception
   {
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting.detection").setLevel(XLevel.TRACE);
//      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      MBeanServer server1 = null;
      try
      {
          server1 = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
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

      NetworkRegistry networkRegistry = new NetworkRegistry();
      registerMBean(server1, networkRegistry, new ObjectName("remoting:type=NetworkRegistry"));
      addNotificationListener(server1, new ObjectName("remoting:type=NetworkRegistry"), this);

      int port = TestUtil.getRandomPort();
      String host = InetAddress.getLocalHost().getHostAddress();
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      locator1 = new InvokerLocator("socket://" + bindAddr + ":" + port);
      log.info("InvokerLocator1: " + locator1);
      Connector connector1 = new Connector(locator1);
      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator1.getProtocol());
      registerMBean(server1, connector1, obj);
      //connector1.create();
      connector1.start();

      MulticastDetector detector1 = new MulticastDetector();
      registerMBean(server1, detector1, new ObjectName("remoting:type=MulticastDetector"));
      // set config info for detector and start it.
      detector1.start();

      log.info("First set started.");

      Thread.sleep(3000);

      log.info("Starting second set.");
      
      MBeanServer server2 = null;
      try
      {
          server2 = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
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
      
      registerMBean(server2, networkRegistry, new ObjectName("remoting:type=NetworkRegistry"));
      addNotificationListener(server2, new ObjectName("remoting:type=NetworkRegistry"), this);

      port = TestUtil.getRandomPort();
      locator2 = new InvokerLocator("socket://" + bindAddr + ":" + port);
      log.info("InvokerLocator2: " + locator2);
      Connector connector2 = new Connector(locator2);
      ObjectName obj2 = new ObjectName("jboss.remoting:type=Connector,transport=" + locator2.getProtocol());
      registerMBean(server2, connector2, obj2);
      //connector2.create();
      connector2.start();

      MulticastDetector detector2 = new MulticastDetector();
      registerMBean(server2, detector2, new ObjectName("remoting:type=MultiplexDetector"));
      // set config info for detector and start it.
      detector2.start();

      log.info("Second set started.");

      Thread.sleep(5000);

      // should have detected both new locators
      assertEquals(2, numOfAdded);

      log.info("Stopping first set.");
      connector1.stop();
      connector1.destroy();
      detector1.stop();
      log.info("First set stopped.");

      //DEBUG
//      Thread.sleep(6000000);

      Thread.sleep(15000);

      // should have detected first set stopped
      // thus leaving only one valid locator
      assertEquals(1, numOfUpdated);

      log.info("Stopping second set.");
      connector2.stop();
      connector2.destroy();
      detector2.stop();
      log.info("Stopped second set.");

      Thread.sleep(15000);

      // number of update locators should remain 1
      // as no detector left to tell network registry
      // of a change.
      assertEquals(1, numOfUpdated);

   }


   public synchronized void handleNotification(Notification notification, Object o)
   {
      log.info("Received notification: " + notification);
      if (notification instanceof NetworkNotification)
      {
         int tempAdded = 0;
         NetworkNotification netNot = (NetworkNotification) notification;
         if(NetworkNotification.SERVER_ADDED.equals(netNot.getType()))
         {
            InvokerLocator[] locators = netNot.getLocator();
            for (int i = 0; i < locators.length; i++)
            {
               if (locators[i].isSameEndpoint(locator1) || locators[i].isSameEndpoint(locator2))
                  tempAdded++;
            }
            if (tempAdded > 0)
            {
               numOfAdded = tempAdded;
               log.info("server added.  num of locators added = " + numOfAdded);
            }
         }
         else if(NetworkNotification.SERVER_UPDATED.equals(netNot.getType()))
         {
            int tempUpdated = 0;
            InvokerLocator[] locators = netNot.getLocator();
            for (int i = 0; i < locators.length; i++)
            {
               if (locators[i].isSameEndpoint(locator1) || locators[i].isSameEndpoint(locator2))
                  tempUpdated++;
            }
            if (tempUpdated > 0)
            {
               numOfUpdated = tempUpdated;
               log.info("server updated.  num of locators in update = " + numOfUpdated);
            }
         }
         ServerInvokerMetadata[] serverMetadata = netNot.getServerInvokers();
         log.info(netNot.getIdentity());
         log.info(serverMetadata);
         InvokerLocator[] locators = netNot.getLocator();
         if (locators != null)
         {
            for (int x = 0; x < locators.length; x++)
            {
               log.info(locators[x]);
            }
         }
         subSystem = serverMetadata[0].getSubSystems()[0];

      }
   }
   
   private void registerMBean(final MBeanServer server, final Object obj, final ObjectName name)
   throws Exception
   {
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               server.registerMBean(obj, name);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         log.info(e.getCause());
         throw (Exception) e.getCause();
      } 
   }

   private void addNotificationListener(final MBeanServer server, final ObjectName name, final NotificationListener listener)
   throws Exception
   {
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               server.addNotificationListener(name, listener, null, null);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         log.info(e.getCause());
         throw (Exception) e.getCause();
      }  
   }

}