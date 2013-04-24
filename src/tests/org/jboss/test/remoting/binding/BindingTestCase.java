/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.binding;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * BindingTestCase verifies that the case in which the InvokerLocator host is
 * 0.0.0.0 is handled correctly.
 *  
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class BindingTestCase extends TestCase
{
   /**
    * Verifies correct behavior for InvokerLocator host == 0.0.0.0, where 
    * InvokerLocator is reconstructed with host as localhost name.
    */
   public void testBindingWithLocatorByName() throws Exception
   {
      System.setProperty(InvokerLocator.BIND_BY_HOST, "true");
      int bindPort = PortUtil.findFreePort("0.0.0.0");
      String locatorUrl = "socket://0.0.0.0:" + bindPort;

      Connector connector = new Connector(locatorUrl);
      connector.create();
      connector.start();

      // Verify that the InvokerLocator host is set properly.
      String connectorLocatorUrl = connector.getInvokerLocator();
      System.out.println("connector locator = " + connectorLocatorUrl);
      String hostName = InetAddress.getLocalHost().getHostName();
      assertFalse(-1 == connectorLocatorUrl.indexOf(hostName));

      // Verify that the ServerSocket is bound to address 0.0.0.0.
      ServerInvoker si = connector.getServerInvoker();
      assertTrue(si instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) si;
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(ssi);
      ServerSocket ss = (ServerSocket) serverSockets.get(0);
      assertNotNull(ss);
      System.out.println("ServerSocket bind address: " + ss.getInetAddress());
      InetAddress inetAddress = ss.getInetAddress();
      assertNotNull(inetAddress);
      assertEquals("0.0.0.0", inetAddress.getHostAddress());
      
      connector.stop();
      connector.destroy();
      
      // Make sure ServerInvoker was destroyed, which implies it was reregistered
      // under correct InvokerLocator.
      assertEquals(0, InvokerRegistry.getServerInvokers().length);
   }
   
   /**
    * Verifies correct behavior for InvokerLocator host == 0.0.0.0, where 
    * InvokerLocator is reconstructed with host as localhost address.
    */
   public void testBindingWithLocatorByAddress() throws Exception
   {
      System.setProperty(InvokerLocator.BIND_BY_HOST, "false");
      int bindPort = PortUtil.findFreePort("0.0.0.0");
      String locatorUrl = "socket://0.0.0.0:" + bindPort;

      Connector connector = new Connector(locatorUrl);
      connector.create();
      connector.start();

      // Verify that the InvokerLocator host is set properly.
      String connectorLocatorUrl = connector.getInvokerLocator();
      System.out.println("connector locator = " + connectorLocatorUrl);
      String hostName = InetAddress.getLocalHost().getHostAddress();
      assertFalse(-1 == connectorLocatorUrl.indexOf(hostName));

      // Verify that the ServerSocket is bound to address 0.0.0.0.
      ServerInvoker si = connector.getServerInvoker();
      assertTrue(si instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) si;
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(ssi);
      ServerSocket ss = (ServerSocket) serverSockets.get(0);
      assertNotNull(ss);
      System.out.println("ServerSocket bind address: " + ss.getInetAddress());
      InetAddress inetAddress = ss.getInetAddress();
      assertNotNull(inetAddress);
      assertEquals("0.0.0.0", inetAddress.getHostAddress());
      
      connector.stop();
      connector.destroy();
      
      // Make sure ServerInvoker was destroyed, which implies it was reregistered
      // under correct InvokerLocator.
      assertEquals(0, InvokerRegistry.getServerInvokers().length);
   }
   
   
   /**
    * Verifies correct behavior for XML document with host == 0.0.0.0, where 
    * InvokerLocator is reconstructed with host as localhost name.
    */
   public void testBindingsWithXMLConfigByname() throws Exception
   {
      System.setProperty(InvokerLocator.BIND_BY_HOST, "true");
      int bindPort = PortUtil.findFreePort("0.0.0.0");
   
      String xml = new StringBuffer()
        .append("<mbean code=\"org.jboss.remoting.transport.Connector\"\n")
        .append(" name=\"jboss.messaging:service=Connector,transport=socket\"\n")
        .append(" display-name=\"Connector\">\n")
        .append(" <attribute name=\"Configuration\">\n")
        .append("  <config>\n")
        .append("   <invoker transport=\"socket\">\n")
        .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_ADDRESS_KEY + "\">0.0.0.0</attribute>\n")
        .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_PORT_KEY + "\">" + bindPort + "</attribute>\n")
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
      connector.create();
      connector.start();

      // Verify that the InvokerLocator host is set properly.
      String connectorLocatorUrl = connector.getInvokerLocator();
      System.out.println("connector locator = " + connectorLocatorUrl);
      String hostName = InetAddress.getLocalHost().getHostName();
      assertFalse(-1 == connectorLocatorUrl.indexOf(hostName));

      // Verify that the ServerSocket is bound to address 0.0.0.0.
      ServerInvoker si = connector.getServerInvoker();
      assertTrue(si instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) si;
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(ssi);
      ServerSocket ss = (ServerSocket) serverSockets.get(0);
      assertNotNull(ss);
      System.out.println("ServerSocket bind address: " + ss.getInetAddress());
      InetAddress inetAddress = ss.getInetAddress();
      assertNotNull(inetAddress);
      assertEquals("0.0.0.0", inetAddress.getHostAddress());

      connector.stop();
      connector.destroy();

      // Make sure ServerInvoker was destroyed, which implies it was reregistered
      // under correct InvokerLocator.
      assertEquals(0, InvokerRegistry.getServerInvokers().length);
   }
 
   
   /**
    * Verifies correct behavior for XML document with host == 0.0.0.0, where 
    * InvokerLocator is reconstructed with host as localhost address.
    */
   public void testBindingsWithXMLConfigByAddress() throws Exception
   {
      System.setProperty(InvokerLocator.BIND_BY_HOST, "false");
      int bindPort = PortUtil.findFreePort("0.0.0.0");
   
      String xml = new StringBuffer()
        .append("<mbean code=\"org.jboss.remoting.transport.Connector\"\n")
        .append(" name=\"jboss.messaging:service=Connector,transport=socket\"\n")
        .append(" display-name=\"Connector\">\n")
        .append(" <attribute name=\"Configuration\">\n")
        .append("  <config>\n")
        .append("   <invoker transport=\"socket\">\n")
        .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_ADDRESS_KEY + "\">0.0.0.0</attribute>\n")
        .append("    <attribute name=\"" + ServerInvoker.SERVER_BIND_PORT_KEY + "\">" + bindPort + "</attribute>\n")
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
      connector.create();
      connector.start();

      // Verify that the InvokerLocator host is set properly.
      String connectorLocatorUrl = connector.getInvokerLocator();
      System.out.println("connector locator = " + connectorLocatorUrl);
      String hostName = InetAddress.getLocalHost().getHostAddress();
      assertFalse(-1 == connectorLocatorUrl.indexOf(hostName));

      // Verify that the ServerSocket is bound to address 0.0.0.0.
      ServerInvoker si = connector.getServerInvoker();
      assertTrue(si instanceof SocketServerInvoker);
      SocketServerInvoker ssi = (SocketServerInvoker) si;
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(ssi);
      ServerSocket ss = (ServerSocket) serverSockets.get(0);
      assertNotNull(ss);
      System.out.println("ServerSocket bind address: " + ss.getInetAddress());
      InetAddress inetAddress = ss.getInetAddress();
      assertNotNull(inetAddress);
      assertEquals("0.0.0.0", inetAddress.getHostAddress());

      connector.stop();
      connector.destroy();

      // Make sure ServerInvoker was destroyed, which implies it was reregistered
      // under correct InvokerLocator.
      assertEquals(0, InvokerRegistry.getServerInvokers().length);
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