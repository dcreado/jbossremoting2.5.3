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

package org.jboss.test.remoting.configuration;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;


/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketClientConfigurationTestCase extends TestCase
{
   private String transport = "socket";
   private int serverPort = 6666;
   private int clientPort = 7777;
   private String hostName = null;
   private String hostIP = null;
   private Connector connector = null;

   public void setUp() throws Exception
   {
      hostName = InetAddress.getLocalHost().getHostName();
      hostIP = InetAddress.getLocalHost().getHostAddress();

      connector = new Connector();
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + transport + "\">");
      buf.append("<attribute name=\"numAcceptThreads\">1</attribute>");
      buf.append("<attribute name=\"maxPoolSize\">303</attribute>");
      buf.append("<attribute name=\"clientMaxPoolSize\" isParam=\"true\">304</attribute>");
      buf.append("<attribute name=\"timeout\">60000</attribute>");
      buf.append("<attribute name=\"serverBindAddress\">" + hostName + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + serverPort + "</attribute>");
      buf.append("<attribute name=\"clientConnectAddress\">" + hostIP + "</attribute>");
      buf.append("<attribute name=\"clientConnectPort\">" + clientPort + "</attribute>");
      buf.append("<attribute name=\"enableTcpNoDelay\" isParam=\"true\">false</attribute>");
      buf.append("<attribute name=\"backlog\">200</attribute>");
      buf.append("</invoker>");
      buf.append("<handlers>");
      buf.append("  <handler subsystem=\"mock\">" + MockServerInvocationHandler.class.getName() + "</handler>\n");
      buf.append("</handlers>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      //connector.setInvokerLocator(locator.getLocatorURI());
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.start();
      Thread.currentThread().sleep(2000);

   }

   public void testClientConfiguration() throws Exception
   {

      // make sure the client's view of the locator will be as configured
      ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();

      if(serverInvokers != null && serverInvokers.length > 0)
      {
         InvokerLocator locator = serverInvokers[0].getLocator();
         String locatorHost = locator.getHost();
         int locatorPort = locator.getPort();

         System.out.println("locator host = " + locatorHost);
         System.out.println("locator port = " + locatorPort);
         assertEquals(hostIP, locatorHost);
         assertEquals(clientPort, locatorPort);
      }

      // check for server bind port (assume is the server since no exception thrown before)
      boolean portAvailable = PortUtil.checkPort(serverPort, hostName);
      assertTrue(!portAvailable);

      // make sure can call ont
      Client client = new Client(new InvokerLocator(transport + "://" + hostName + ":" + serverPort));
      client.connect();
      String param = "foobar";
      Object ret = null;
      try
      {
         ret = client.invoke(param);
      }
      catch(Throwable throwable)
      {
         throw new Exception("Call on server failed.", throwable);
      }
      assertEquals(param, ret);

   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      SocketClientConfigurationTestCase testCase = new SocketClientConfigurationTestCase();
      try
      {
         testCase.setUp();
         testCase.testClientConfiguration();
         testCase.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}