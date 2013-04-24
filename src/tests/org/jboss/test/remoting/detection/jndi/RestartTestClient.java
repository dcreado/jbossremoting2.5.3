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

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class RestartTestClient extends TestCase implements NotificationListener
{
   private static Logger log = Logger.getLogger(RestartTestClient.class);
   
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";
   
   protected JNDIDetector detector;
   protected int serversDetected;
   protected boolean invocationSucceeded;
   protected Object lock = new Object();
   protected boolean notified;


   /**
    * Sets up NetworkRegistry and JNDIDetector so we can listen for any additions
    * or removals of remoting servers on the network.
    *
    * @throws Exception
    */
   public void setUp() throws Exception
   {
      // we need an MBeanServer to store our network registry and jndi detector services
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // the registry will house all remoting servers discovered
      NetworkRegistry registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));
      log.info("NetworkRegistry has been created");

      // register class as listener, so know when new server found
      registry.addNotificationListener(this, null, null);
      log.info("NetworkRegistry has added the client as a listener");

      String detectorHost = InetAddress.getLocalHost().getHostName();
      
      // jndi detector will detect new network registries that come online
      detector = new JNDIDetector(getConfiguration());
      // set config info for detector and start it.
      detector.setPort(detectorPort);
      detector.setHost(detectorHost);
      detector.setContextFactory(contextFactory);
      detector.setURLPackage(urlPackage);

      server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));

      for (int i = 0; i < 5; i++)
      {
         try
         {
            detector.start();
            log.info("JNDIDetector has been created and is listening for new NetworkRegistries to come online");
            break;
         }
         catch (Exception e)
         {
            log.info("unable to connect to JDNI: will try again");
            Thread.sleep(2000);
         }
      }
   }

   
   public void tearDown() throws Exception
   {
      detector.stop();
   }
   
   public void testDetections()
   {
      try
      {
         String host = InetAddress.getLocalHost().getHostName();
         
         Socket s = null;
         for (int i = 0; i < 5; i++)
         {
            try
            {
               s = new Socket(host, RestartTestServer.syncPort);
               break;
            }
            catch (Exception e)
            {
               log.info("Unable to connect to " + host + ":" + RestartTestServer.syncPort);
               log.info("Will try again");
               try
               {
                  Thread.sleep(2000);
               }
               catch (InterruptedException ignored) {}
            }
         }
         InputStream is = s.getInputStream();
         OutputStream os = s.getOutputStream();
         
         // Wait until server has been started.
         is.read();
         waitOnDetection();
         assertEquals(1, serversDetected);
         assertTrue(invocationSucceeded);
         log.info("PASSED first detection test");
         invocationSucceeded = false;
         
         // Tell server to shut down.
         os.write(5);
         waitOnDetection();
         assertEquals(0, serversDetected);
         log.info("PASSED second detection test");
         
         // Tell server to restart.
         os.write(7);
         waitOnDetection();
         assertEquals(1, serversDetected);
         assertTrue(invocationSucceeded);
         log.info("PASSED third detection test");

         // Tell server test is over.
         os.write(9);
      }
      catch (Exception e)
      {
         log.error(e);
         e.printStackTrace();
         fail();
      }
   }

   /**
    * Callback method from the broadcaster MBean this listener implementation is registered to. When a new server
    * is detected, a welcome message will immediately be sent to the newly discovered server.
    *
    * @param notification the notification object
    * @param handback     the handback object given to the broadcaster upon listener registration
    */
   public void handleNotification(Notification notification, Object handback)
   {
      try
      {
         // check to see if network notification
         if(notification instanceof NetworkNotification)
         {
            log.info("GOT A NETWORK-REGISTRY NOTIFICATION: " + notification.getType());
            
            NetworkNotification networkNotification = (NetworkNotification) notification;
            
            if(NetworkNotification.SERVER_ADDED.equals(networkNotification.getType()))
            { // notification is for new servers being added
               log.info("New server(s) have been detected - getting locators and sending welcome messages");
               InvokerLocator[] locators = networkNotification.getLocator();
               for(int x = 0; x < locators.length; x++)
               {
                  try
                  {
                     serversDetected++;
                     
                     // get the new found server's locator and invoke a call
                     InvokerLocator newServerLocator = locators[x];
                     log.info("detected: " + newServerLocator);
                     invocationSucceeded = false;
                     makeInvocation(newServerLocator.getLocatorURI());
                     invocationSucceeded = true;
                  }
                  catch(Throwable throwable)
                  {
                     throwable.printStackTrace();
                  }
               }
            }
            else if(NetworkNotification.SERVER_REMOVED.equals(networkNotification.getType()))
            { // notification is for old servers that have gone down
               InvokerLocator[] locators = networkNotification.getLocator();
               for(int x = 0; x < locators.length; x++)
               {
                  serversDetected--;
                  log.info("It has been detected that a server has gone down with a locator of: " + locators[x]);
               }
            }
         }
         
         return;
      }
      finally
      {
         notifyDetection();
      }
   }
   
   
   protected void waitOnDetection() throws InterruptedException
   {
      synchronized (lock)
      {  
         try
         {
            if (notified)
               return;
            
            lock.wait();
         }
         finally
         {
            notified = false;
         }
      }
   }
   
   
   protected void notifyDetection()
   {
      synchronized (lock)
      {
         notified = true;
         lock.notify();
      }
   }

   /**
    * Make call on remoting server based on locator uri provided.
    *
    * @param locatorURI the URI of the remote server we want to send the message to
    * @throws Throwable
    */
   public void makeInvocation(String locatorURI) throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      log.info("Sending welcome message to remoting server with locator uri of: " + locatorURI);

      Client remotingClient = new Client(locator, getConfiguration());
      remotingClient.connect();
      Object response = remotingClient.invoke("Welcome Aboard!", null);

      log.info("The newly discovered server sent this response to our welcome message: " + response);

      remotingClient.disconnect();
      return;
   }

   
   /**
    * @return configuration map for JNDIDetector
    */
   protected Map getConfiguration()
   {
      return new HashMap();
   }
}
