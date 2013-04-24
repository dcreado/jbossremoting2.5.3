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

import java.net.InetAddress;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;

/**
 * Just tests that detector A sees detector B when B comes online then off.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class JNDIDetectorTest2 extends ServerTestCase
{
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";

   private JNDIDetector detector;
   private Connector connector;
   private NetworkRegistry registry;

   private static final Logger log = Logger.getLogger(JNDIDetectorTest2.class);

   public void setUp() throws Exception
   {
      String detectorHost = InetAddress.getLocalHost().getHostName();

      detector = new JNDIDetector();
      detector.setCleanDetectionNumber(2);

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      //registry = NetworkRegistry.getInstance();
      registry = JNDIDetectorTest2.TestNetworkRegistry.createNetworkRegistry();
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

      MockServerInvocationHandler handler = new MockServerInvocationHandler();
      connector.addInvocationHandler("mock", handler);

      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
      server.registerMBean(connector, obj);
      //connector.create();
      connector.start();

      //Need to set new domain for identity
      server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));

      // set config info for detector and start it.
      detector.setPort(detectorPort);
      detector.setHost(detectorHost);
      detector.setContextFactory(contextFactory);
      detector.setURLPackage(urlPackage);
      detector.start();

   }

   public void testDetector2() throws Exception
   {
      Thread.currentThread().sleep(10000);

      NetworkInstance[] server = registry.getServers();
      if(server != null && server.length > 0)
      {
         System.out.println("PASS - got more than zero servers detected.");
         log.info("PASS - got more than zero servers detected.");
         assertTrue("Detected server.", true);
      }
      else
      {
         System.out.println("FAIL - did not detect any other server.");
         log.info("FAIL - did not detect any other server.");
         assertTrue("Did not detect any servers.", false);
      }

      if(connector != null)
      {
         connector.stop();
         connector.destroy();
         connector = null;
      }
      if(detector != null)
      {
         detector.stop();
         detector = null;
      }
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
         connector = null;
      }
      if(detector != null)
      {
         detector.stop();
         detector = null;
      }

   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);

      try
      {
         JNDIDetectorTest2 test = new JNDIDetectorTest2();
         test.setUp();
         test.testDetector2();
         test.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

   }

   private static class TestNetworkRegistry extends NetworkRegistry
   {
      public static NetworkRegistry createNetworkRegistry()
      {
         return new JNDIDetectorTest2.TestNetworkRegistry();
      }
   }

}