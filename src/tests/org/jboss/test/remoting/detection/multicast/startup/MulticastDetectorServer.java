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
package org.jboss.test.remoting.detection.multicast.startup;

import java.net.InetAddress;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MulticastDetectorServer extends ServerTestCase
{
   private MulticastDetector detector;
   private Connector connector;
   private NetworkRegistry registry;

   public void setUp() throws Exception
   {
      detector = new MulticastDetector();

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

      int port = TestUtil.getRandomPort();
      System.out.println("port = " + port);

      String host = InetAddress.getLocalHost().getHostAddress();
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      InvokerLocator locator = new InvokerLocator("socket://" + bindAddr + ":" + port);

      System.out.println("Starting remoting server with locator uri of: " + locator.getLocatorURI());
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      TestInvocationHandler handler = new TestInvocationHandler();
      connector.addInvocationHandler("mock", handler);

      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
      server.registerMBean(connector, obj);
      //connector.create();
      connector.start();

      //Need to set new domain for identity
      server.registerMBean(detector, new ObjectName("remoting:type=MulticastDetector"));

      detector.start();

   }

   public void tearDown() throws Exception
   {
      if (detector != null)
      {
         detector.stop();
      }
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);

      try
      {
         MulticastDetectorServer test = new MulticastDetectorServer();
         test.setUp();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return "MulticastDetectorServer";
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}