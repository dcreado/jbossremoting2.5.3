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
package org.jboss.test.remoting.detection.jndi.startup;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jnp.server.Main;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class JNDIDetectorServer extends ServerTestCase
{
   private int detectorPort = 1099;
   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";

   private JNDIDetector detector;
   private Connector connector;
   private NetworkRegistry registry;

   public void setUp() throws Exception
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

      String detectorHost = InetAddress.getLocalHost().getHostName();

      detector = new JNDIDetector();
      detector.setCleanDetectionNumber(2);

      System.setProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));
      System.out.println("jboss.identity = " + System.getProperty("jboss.identity"));

      MBeanServer server = MBeanServerFactory.createMBeanServer();

      registry = NetworkRegistry.getInstance();
      server.registerMBean(registry, new ObjectName("remoting:type=NetworkRegistry"));

      port = TestUtil.getRandomPort();
      System.out.println("port = " + port);

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
      Thread.sleep(10000);
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
         JNDIDetectorServer test = new JNDIDetectorServer();
         test.setUp();
      }
      catch (Exception e)
      {
         e.printStackTrace();
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