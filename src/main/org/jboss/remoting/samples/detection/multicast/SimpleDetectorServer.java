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

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.transport.Connector;

/**
 * A simple JBoss/Remoting remoting server.  This uses an inner class SampleInvocationHandler as the invocation
 * target handler class.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 */
public class SimpleDetectorServer
{
   // Default locator values - command line args can override transport and port
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   /**
    * Sets up NetworkRegistry and MulticastDetector so will can register ourselves on the network.  We could also
    * use these to listen ourselves for any additions or removals of remoting servers on the network.
    *
    * @throws Exception
    */
   public void setupDetector()
         throws Exception
   {
      // we need an MBeanServer to store our network registry and multicast detector services
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      // multicast detector will detect new network registries that come online
      MulticastDetector detector = new MulticastDetector();
      server.registerMBean(detector, new ObjectName("remoting:type=MulticastDetector"));
      detector.start();
      println("MulticastDetector has been created and is listening for new NetworkRegistries to come online");

      return;
   }

   /**
    * Sets up our JBoss/Remoting server by creating our Connector on the given locator URI and installing our
    * invocation handler that will handle incoming messages.
    *
    * @param locatorURI defines our server endpoing
    * @throws Exception
    */
   public void setupServer(String locatorURI)
         throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      println("Starting remoting server with locator uri of: " + locatorURI);
      Connector connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();

      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);

      println("Added our invocation handler; we are now ready to begin accepting messages from clients");

      connector.start();

      return;
   }

   /**
    * Can pass transport and port to be used as parameters. Valid transports are 'rmi' and 'socket'.
    *
    * @param args transport and port number
    */
   public static void main(String[] args)
   {
      // get system property -Dargs that is in format "transport:port"
      String prop = System.getProperty("args");
      if(prop != null)
      {
         try
         {
            transport = prop.substring(0, prop.indexOf("-"));
            port = Integer.parseInt(prop.substring(prop.indexOf("-") + 1));
         }
         catch(NumberFormatException nfe)
         {
            println("INVALID ARGUMENTS: Bad port from property args: " + prop);
            System.exit(1);
         }
         catch(Exception e)
         {
            println("INVALID ARGUMENTS: -Dargs property must be in the form '{socket|rmi}-{port#}': " + prop);
            System.exit(1);
         }
      }

      // command line args override defaults and system property
      if((args != null) && (args.length != 0))
      {
         if(args.length == 2)
         {
            transport = args[0];
            port = Integer.parseInt(args[1]);
         }
         else
         {
            println("INVALID ARGUMENTS: Usage: " + SimpleDetectorServer.class.getName()
                    + " [rmi|socket <port>]");
            System.exit(1);
         }
      }

      println("Starting JBoss/Remoting server... to stop this server, kill it manually via Control-C");

      String locatorURI = transport + "://" + host + ":" + port;
      println("This server's endpoint will be: " + locatorURI);

      SimpleDetectorServer server = new SimpleDetectorServer();
      try
      {
         server.setupDetector();
         server.setupServer(locatorURI);

         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

      println("Stopping JBoss/Remoting server");
   }

   /**
    * Outputs a message to stdout.
    *
    * @param msg the message to output
    */
   public static void println(String msg)
   {
      System.out.println(new java.util.Date() + ": [SERVER]: " + msg);
   }

   /**
    * Simple invocation handler implementation.  This is the handler that processes incoming messages from clients.
    */
   public static class SampleInvocationHandler
         implements ServerInvocationHandler
   {
      /**
       * This is the method that is called when a new message comes in from a client.
       *
       * @param invocation the incoming client invocation, encapsulates the message object
       * @return the response object we send back to the client.
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation)
            throws Throwable
      {
         // Print out the invocation request
         String msg = invocation.getParameter().toString();

         println("RECEIVED A CLIENT MESSAGE: " + msg);

         String response = "Server received your message that said [" + msg + "]";

         if(msg.indexOf("Welcome") > -1)
         {
            response = "Received your welcome message.  Thank you!";
         }

         println("Returning the following message back to the client: " + response);

         return response;
      }

      /**
       * Adds a callback handler that will listen for callbacks from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as we do not handle callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as we do not handle callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as we do not need a reference to the MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as we do not need a reference back to the server invoker
      }
   }
}
