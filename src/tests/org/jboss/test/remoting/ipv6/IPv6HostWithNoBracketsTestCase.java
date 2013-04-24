/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.ipv6;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.util.SecurityUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Unit tests for JBREM-1164.
 * 
 * @author <a href="mailto:bclare@tpg.com.au">Ben Clare</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * 
 * @version $Rev$
 * <p>
 * Copyright Dec 20, 2009
 * </p>
 */
public class IPv6HostWithNoBracketsTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(IPv6HostWithNoBracketsTestCase.class);
   private static boolean firstTime = true;
   private static int port = 8080;


   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }


   public void tearDown()
   {
   }


   public void testXMLWithoutBrackets() throws Throwable
   {
      log.info("entering " + getName());

      assertTrue(doXMLTest("::", ++port));
      assertTrue(doXMLTest("::1", ++port));
      assertTrue(doXMLTest("0:0:0:0:0:0:0:1", ++port));
      assertTrue(doXMLTest("0:0:0:0:0:0:127.0.0.1", ++port));
      assertTrue(doXMLTest("3ffe:1900:4545:3:200:f8ff:fe21:67cf", ++port));
      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doXMLTest("3ffe:1900:4545:3:200:f8ff:fe21:67cf%5", ++port));
      }

      log.info(getName() + " PASSES");
   }


   public void testXMLWithBrackets() throws Throwable
   {
      log.info("entering " + getName());

      assertTrue(doXMLTest("[::]", ++port));
      assertTrue(doXMLTest("[::1]", ++port));
      assertTrue(doXMLTest("[0:0:0:0:0:0:0:1]", ++port));
      assertTrue(doXMLTest("[0:0:0:0:0:0:127.0.0.1]", ++port));
      assertTrue(doXMLTest("[3ffe:1900:4545:3:200:f8ff:fe21:67cf]", ++port));
      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doXMLTest("[3ffe:1900:4545:3:200:f8ff:fe21:67cf%5]", ++port));
      }

      log.info(getName() + " PASSES");
   }


   public void testXMLWithoutHost() throws Exception
   {
      log.info("entering " + getName());

      String xml = new StringBuffer()
      .append("<mbean code=\"org.jboss.remoting.transport.Connector\"\n")
      .append(" name=\"jboss.messaging:service=Connector,transport=socket\"\n")
      .append(" display-name=\"Connector\">\n")
      .append(" <attribute name=\"Configuration\">\n")
      .append("  <config>\n")
      .append("   <invoker transport=\"socket\">\n")
      .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_PORT_KEY + "\">" + port + "</attribute>\n")
      .append("    <attribute name=\"timeout\" isParam=\"true\">10000</attribute>\n")
      .append("   </invoker>\n")
      .append("   <handlers>\n")
      .append("    <handler subsystem=\"test\">" + SampleInvocationHandler.class.getName() + "</handler>\n")
      .append("   </handlers>\n")
      .append("  </config>\n")
      .append(" </attribute>\n")
      .append("</mbean>\n").toString();
      Connector connector = new Connector();
      ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      Element element =  doc.getDocumentElement();
      connector.setConfiguration(element);
      try
      {
         connector.create();
         InvokerLocator expected = new InvokerLocator("socket://" + getLocalHost() + ":" + port + "/?timeout=10000");
         InvokerLocator actual = connector.getLocator();
         log.info("Expected: " + expected);
         log.info("Actual:   " + actual);InetAddress.getLocalHost();
         assertEquals(expected, actual);
      }
      catch (Exception e)
      {
         log.error("Exception caught " + e.getMessage());
      }
      finally
      {
         connector.stop();
      }

      log.info(getName() + " PASSES");
   }


   
   public void testServerConfigurationWithoutBrackets() throws Throwable
   {
      log.info("entering " + getName());

      assertTrue(doServerConfigurationTest("::", ++port));
      assertTrue(doServerConfigurationTest("::1", ++port));
      assertTrue(doServerConfigurationTest("0:0:0:0:0:0:0:1", ++port));
      assertTrue(doServerConfigurationTest("0:0:0:0:0:0:127.0.0.1", ++port));
      assertTrue(doServerConfigurationTest("3ffe:1900:4545:3:200:f8ff:fe21:67cf", ++port));
      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doServerConfigurationTest("3ffe:1900:4545:3:200:f8ff:fe21:67cf%5", ++port));
      }
      
      log.info(getName() + " PASSES");
   }


   public void testServerConfigurationWithBrackets() throws Throwable
   {
      log.info("entering " + getName());

      assertTrue(doServerConfigurationTest("[::]", ++port));
      assertTrue(doServerConfigurationTest("[::1]", ++port));
      assertTrue(doServerConfigurationTest("[0:0:0:0:0:0:0:1]", ++port));
      assertTrue(doServerConfigurationTest("[0:0:0:0:0:0:127.0.0.1]", ++port));
      assertTrue(doServerConfigurationTest("[3ffe:1900:4545:3:200:f8ff:fe21:67cf]", ++port));
      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doServerConfigurationTest("[3ffe:1900:4545:3:200:f8ff:fe21:67cf%5]", ++port));
      }
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testServerConfigurationWithoutHost() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create ServerConfiguration.
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();

      // Add invokerLocatorParameters.
      locatorConfig.put("serverBindPort", Integer.toString(port));
      locatorConfig.put("timeout", "10000");
      config.setInvokerLocatorParameters(locatorConfig);

      // Add invocation handler.
      Map handlers = new HashMap();
      ServerInvocationHandler handler = new SampleInvocationHandler();
      handlers.put("test", handler); 
      config.setInvocationHandlers(handlers);

      // Create Connector.
      Connector connector = new Connector();
      try
      {
         connector.setServerConfiguration(config);
         connector.create();
         InvokerLocator expected = new InvokerLocator("socket://" + getLocalHost() + ":" + port + "/?timeout=10000");
         InvokerLocator actual = connector.getLocator();
         log.info("Expected: " + expected);
         log.info("Actual:   " + actual);
         assertEquals(expected, actual);
      }
      catch (Exception e)
      {
         log.error("Exception caught ", e);
      }
      finally
      {
         connector.stop();
      }
      
      log.info(getName() + " PASSES");
   }


   protected boolean doXMLTest(String host, int port) throws Exception
   {
      String xml = new StringBuffer()
      .append("<mbean code=\"org.jboss.remoting.transport.Connector\"\n")
      .append(" name=\"jboss.messaging:service=Connector,transport=socket\"\n")
      .append(" display-name=\"Connector\">\n")
      .append(" <attribute name=\"Configuration\">\n")
      .append("  <config>\n")
      .append("   <invoker transport=\"socket\">\n")
      .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_ADDRESS_KEY + "\">" + host + "</attribute>\n")
      .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_PORT_KEY + "\">" + port + "</attribute>\n")
      .append("    <attribute name=\"timeout\" isParam=\"true\">10000</attribute>\n")
      .append("   </invoker>\n")
      .append("   <handlers>\n")
      .append("    <handler subsystem=\"test\">" + SampleInvocationHandler.class.getName() + "</handler>\n")
      .append("   </handlers>\n")
      .append("  </config>\n")
      .append(" </attribute>\n")
      .append("</mbean>\n").toString();
      Connector connector = new Connector();
      ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      Element element =  doc.getDocumentElement();
      connector.setConfiguration(element);
      try
      {
         connector.create();
         InvokerLocator expected = new InvokerLocator("socket://" + fixHostnameForURL(host) + ":" + port + "/?timeout=10000");
         InvokerLocator actual = connector.getLocator();
         log.info("Expected: " + expected);
         log.info("Actual:   " + actual);
         assertEquals(expected, actual);
         return true;
      }
      catch (Exception e)
      {
         log.error("Exception caught ", e);
         return false;
      }
      finally
      {
         connector.stop();
      }
   }


   protected boolean doServerConfigurationTest(String host, int port) throws Exception
   {
      // Create ServerConfiguration.
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();

      // Add invokerLocatorParameters.
      locatorConfig.put("serverBindAddress", host);
      locatorConfig.put("serverBindPort", Integer.toString(port));
      locatorConfig.put("timeout", "10000");
      config.setInvokerLocatorParameters(locatorConfig);

      // Add invocation handler.
      Map handlers = new HashMap();
      ServerInvocationHandler handler = new SampleInvocationHandler();
      handlers.put("test", handler); 
      config.setInvocationHandlers(handlers);

      // Create Connector.
      Connector connector = new Connector();
      try
      {
         connector.setServerConfiguration(config);
         connector.create();
         InvokerLocator expected = new InvokerLocator("socket://" + fixHostnameForURL(host) + ":" + port + "/?timeout=10000");
         InvokerLocator actual = connector.getLocator();
         log.info("Expected: " + expected);
         log.info("Actual:   " + actual);
         assertEquals(expected, actual);
         return true;
      }
      catch (Exception e)
      {
         log.error("Exception caught ", e);
         return false;
      }
      finally
      {
         connector.stop();
      }
   }


   static protected String fixHostnameForURL(String host)
   {
      if (host == null)
         return host ;

      // if the hostname is an IPv6 literal, enclose it in brackets
      if (host.indexOf(':') != -1 && host.indexOf("[") == -1)
      {
         System.out.println("HOST: [" + host + "]");
         return "[" + host + "]" ;
      }
      else
      {
         return host;
      }
   } 


   static protected String getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            System.setProperty("java.net.preferIPv6Addresses", "true");
            return InetAddress.getLocalHost().getHostAddress();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1").getHostAddress();
         }
      }

      try
      {
         return (String) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  System.setProperty("java.net.preferIPv6Addresses", "true");
                  return InetAddress.getLocalHost().getHostAddress();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1").getHostAddress();
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }


   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}