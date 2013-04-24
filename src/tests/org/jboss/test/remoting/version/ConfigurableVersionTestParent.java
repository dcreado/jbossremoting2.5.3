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
package org.jboss.test.remoting.version;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-764.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 6, 2008
 * </p>
 */
public abstract class ConfigurableVersionTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(ConfigurableVersionTestParent.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
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
   
   
   public void testVersions() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start servers.
      Connector connector1 = setupServer(Version.VERSION_1);
      Connector connector2 = setupServer(Version.VERSION_2);
      Connector connector22 = setupServer(Version.VERSION_2_2);
      Connector connector0 = setupServer(-1);
      
      // Create clients.
      Client client1 = setupClient(connector1);
      Client client2 = setupClient(connector2);
      Client client22 = setupClient(connector22);
      Client client0 = setupClient(connector0);
      
      // Test connections.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("abc", client2.invoke("abc"));
      assertEquals("abc", client22.invoke("abc"));
      assertEquals("abc", client0.invoke("abc"));
      
      client1.disconnect();
      client2.disconnect();
      client22.disconnect();
      client0.disconnect();
      
      connector1.stop();
      connector2.stop();
      connector22.stop();
      connector0.stop();
      
      log.info(getName() + " PASSES");
   }
   
   
   abstract protected String getTransport();
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected Connector setupServer(int version) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?";
      
      if (version > 0)
      {
         locatorURI += Remoting.REMOTING_VERSION + "=" + version;
      }
      else
      {
         locatorURI += "x=y";
      }
      
      String parameters = System.getProperty("remoting.metadata");
      if (parameters != null)
      {
         locatorURI += "&" + parameters;
      }
      
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      return connector;
   }
   
   
   protected Client setupClient(Connector connector) throws Exception
   {
      InvokerLocator locator = connector.getLocator();
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(locator, config);
      client.connect();
      log.info("client is connected to: " + locator);
      return client;
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}