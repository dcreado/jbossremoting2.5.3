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

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jnp.server.Main;

import junit.framework.TestCase;

/**
 * Just tests that detector A sees detector B when B comes online then off.
 * This is a JUnit test, but will need to run JNDIDetectorTest2 at same time
 * in order to work properly.  Can also just run JNDIDetectorTestCase as
 * test harness to run both JNDIDetectorTest1 and JNDIDetectorTest2 (as regular JUnit test).
 * See the main for the arguments required (based on DistributedTestCase confines).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class JNDIDetectorTest1 extends TestCase
{
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";

   private JNDIDetector detector;
   private Connector connector;
   private NetworkRegistry registry;

   private static final Logger log = Logger.getLogger(JNDIDetectorTest1.class);

   public void testDetector() throws Exception
   {
      Thread.currentThread().sleep(5000);

      NetworkInstance[] server = registry.getServers();
      if(server != null && server.length > 0)
      {
         System.out.println("PASS - more than zero servers detected.");
         log.info("PASS - more than zero servers detected.");
         assertTrue("Got detection.", true);
      }
      else
      {
         System.out.println("FAIL - no servers detected");
         log.info("FAIL - no servers detected");

         assertTrue("Did not detect servers.", false);
      }

      Thread.currentThread().sleep(60000);

      server = registry.getServers();
      if(server == null || server.length == 0)
      {
         System.out.println("PASS - no servers detected.");
         log.info("PASS - no servers detected.");
         assertTrue("No servers detected.", true);
      }
      else
      {
         System.out.println("FAIL - all servers should have been removed.");
         log.info("FAIL - all servers should have been removed.");
         assertTrue("Found a server, but should have been removed.", false);
      }

   }

   public void setUp() throws Exception
   {
      String detectorHost = InetAddress.getLocalHost().getHostName();

      detector = new JNDIDetector();
      detector.setCleanDetectionNumber(2);

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      //registry = NetworkRegistry.getInstance();
      registry = JNDIDetectorTest1.TestNetworkRegistry.createNetworkRegistry();
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

   public void tearDown() throws Exception
   {
      if(detector != null)
      {
         detector.stop();
      }
      if(connector != null)
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
         // start JNDI server
         Object namingBean = null;
         Class namingBeanImplClass = null;
         try
         {
            namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
            namingBean = namingBeanImplClass.newInstance();
            Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
            setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            startMethod.invoke(namingBean, new Object[] {});
         }
         catch (Exception e)
         {
            SimpleJNDIServer.println("Cannot find NamingBeanImpl: must be running jdk 1.4");
         }
         
         int port = 1099;
         //String host = "localhost";
         String host = InetAddress.getLocalHost().getHostName();
         Main JNDIServer = new Main();
         if (namingBean != null)
         {
            Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
            Method setNamingInfoMethod = JNDIServer.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
            setNamingInfoMethod.invoke(JNDIServer, new Object[] {namingBean});
         }
         JNDIServer.setPort(port);
         JNDIServer.setBindAddress(host);
         JNDIServer.start();
         System.out.println("Started JNDI server on " + host + ":" + port);

         JNDIDetectorTest1 test = new JNDIDetectorTest1();
         test.setUp();
         test.testDetector();
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
         return new JNDIDetectorTest1.TestNetworkRegistry();
      }
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}