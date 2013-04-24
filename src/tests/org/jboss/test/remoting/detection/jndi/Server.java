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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author Michael Voss
 */
public class Server extends ServerTestCase
{
   private static Connector connector = null;
   private MBeanServer server;
   private JNDIDetector detector;
   private Logger logger;

   private String jndiAddress = null;
   private int jndiPort = 2410;
   private String port = "3001";


   private void init()
   {
      String localHost = "";
      try
      {
         jndiAddress = localHost = InetAddress.getLocalHost().getHostAddress();
      }
      catch (UnknownHostException uhe)
      {
         uhe.printStackTrace();
         System.exit(1);
      }

      try
      {


         connector = new Connector();
         InvokerLocator locator = new InvokerLocator("multiplex://" + localHost + ":" + port);
         connector.setInvokerLocator(locator.getLocatorURI());
         connector.create();
         try
         {
            connector.addInvocationHandler("sample", new SampleInvocationHandler());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            System.exit(1);
         }
         connector.start();

         server.registerMBean(connector, new ObjectName("jboss.remoting:type=Connector"));


      }
      catch (Exception e)
      {
         e.printStackTrace();
      }


   }


   private void registerJNDI()
   {

      detector = new JNDIDetector();

      try
      {
         server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));
      }
      catch (Exception ignored)
      {
      }

      detector.setPort(jndiPort);
      detector.setHost(jndiAddress);
      try
      {
         detector.start();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }


   public void setUp()
   {
      logger = Logger.getLogger(getClass());
      logger.setLevel((Level) Level.DEBUG);

      server = MBeanServerFactory.createMBeanServer();
      init();
      registerJNDI();


   }


   public void tearDown()
   {
      try
      {

         server.unregisterMBean(new ObjectName("remoting:type=JNDIDetector"));
         if (detector != null)
         {
            try
            {
               detector.stop();
            }
            catch (Exception ignored)
            {
            }
         }
         if (connector != null)
         {
            try
            {
               connector.stop();
            }
            catch (Exception ignored)
            {
            }

            connector = null;
            server.unregisterMBean(new ObjectName("jboss.remoting:type=Connector"));
            Thread.sleep(1000);

         }
      }
      catch (Exception e)
      {
      }

   }


   public static void main(String[] args)
   {

      Server server = new Server();
      try
      {
         server.setUp();

         while (true)
         {
            Thread.sleep(1000);
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


   public static class SampleInvocationHandler implements ServerInvocationHandler
   {

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return null;
      }


      public void addListener(InvokerCallbackHandler callbackHandler)
      {

      }


      public void removeListener(InvokerCallbackHandler callbackHandler)
      {

      }


      public void setMBeanServer(MBeanServer server)
      {

      }

      public void setInvoker(ServerInvoker invoker)
      {

      }

   }
}