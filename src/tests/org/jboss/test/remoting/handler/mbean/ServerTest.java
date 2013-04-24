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

package org.jboss.test.remoting.handler.mbean;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerTest extends ServerTestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI = transport + "://" + host + ":" + port;
   private Connector connector;

   // String to be returned from invocation handler upon client invocation calls.
   public static final String RESPONSE_VALUE = "This is the return from the TestServer invocation";

   public void setupServer() throws Exception
   {
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();

      server.registerMBean(connector, new ObjectName("test:type=connector,transport=socket"));

      // now create Mbean handler and register with mbean server
      MBeanHandler handler = new MBeanHandler();
      ObjectName objName = new ObjectName("test:type=handler");
      server.registerMBean(handler, objName);

      connector.addInvocationHandler("test", objName);
   }

   public void setUp() throws Exception
   {
      setupServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      ServerTest serverTest = new ServerTest();
      try
      {
         serverTest.setUp();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}