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

package org.jboss.remoting.samples.detection.multicast;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkNotification;
import org.jboss.remoting.network.NetworkRegistry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Simple remoting client that uses detection to discover the remoting server to make invocation on. Note that this
 * class is a standard JMX NotificationListener so it can listen for discovery JMX notifications coming from the
 * NetworkRegistry.  This is how the NetworkRegistry tells us when new servers have come online and when dead
 * servers go offline.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 */
public class SimpleDetectorClient
      implements NotificationListener
{
   /**
    * Sets up NetworkRegistry and MulticastDetector so we can listen for any additions or removals of remoting
    * servers on the network.
    *
    * @throws Exception
    */
   public void setupDetector()
         throws Exception
   {
      // we need an MBeanServer to store our network registry and multicast detector services
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // the registry will house all remoting servers discovered
      NetworkRegistry registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));
      println("NetworkRegistry has been created");

      // register class as listener, so know when new server found
      registry.addNotificationListener(this, null, null);
      println("NetworkRegistry has added the client as a listener");

      // multicast detector will detect new network registries that come online
      MulticastDetector detector = new MulticastDetector();
      server.registerMBean(detector, new ObjectName("remoting:type=MulticastDetector"));
      detector.start();
      println("MulticastDetector has been created and is listening for new NetworkRegistries to come online");

      return;
   }

   /**
    * Callback method from the broadcaster MBean this listener implementation is registered to. When a new server
    * is detected, a welcome message will immediately be sent to the newly discovered server.
    *
    * @param notification the notification object
    * @param handback     the handback object given to the broadcaster upon listener registration
    */
   public void handleNotification(Notification notification,
                                  Object handback)
   {
      // check to see if network notification
      if(notification instanceof NetworkNotification)
      {
         println("GOT A NETWORK-REGISTRY NOTIFICATION: " + notification.getType());

         NetworkNotification networkNotification = (NetworkNotification) notification;

         if(NetworkNotification.SERVER_ADDED.equals(networkNotification.getType()))
         { // notification is for new servers being added
            println("New server(s) have been detected - getting locators and sending welcome messages");
            InvokerLocator[] locators = networkNotification.getLocator();
            for(int x = 0; x < locators.length; x++)
            {
               try
               {
                  // get the new found server's locator and invoke a call
                  InvokerLocator newServerLocator = locators[x];
                  makeInvocation(newServerLocator.getLocatorURI());
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
               println("It has been detected that a server has gone down with a locator of: " + locators[x]);
            }
         }
      }

      return;
   }

   /**
    * Make call on remoting server based on locator uri provided.
    *
    * @param locatorURI the URI of the remote server we want to send the message to
    * @throws Throwable
    */
   public void makeInvocation(String locatorURI)
         throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      println("Sending welcome message to remoting server with locator uri of: " + locatorURI);

      Client remotingClient = new Client(locator);
      remotingClient.connect();
      Object response = remotingClient.invoke("Welcome Aboard!", null);

      println("The newly discovered server sent this response to our welcome message: " + response);

      return;
   }

   /**
    * Starts the JBoss/Remoting client.
    *
    * @param args unused
    */
   public static void main(String[] args)
   {
      println("Starting JBoss/Remoting client... to stop this client, kill it manually via Control-C");
      SimpleDetectorClient client = new SimpleDetectorClient();
      try
      {
         client.setupDetector();

         // let this client run forever - welcoming new servers when then come online
         while(true)
         {
            Thread.sleep(1000);
         }
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }

      println("Stopping JBoss/Remoting client");
   }

   /**
    * Outputs a message to stdout.
    *
    * @param msg the message to output
    */
   public static void println(String msg)
   {
      System.out.println(new java.util.Date() + ": [CLIENT]: " + msg);
   }
}
