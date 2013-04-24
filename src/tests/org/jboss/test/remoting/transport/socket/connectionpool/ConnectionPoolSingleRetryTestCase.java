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
package org.jboss.test.remoting.transport.socket.connectionpool;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;


/**
 * 
 * Unit test for JBREM-911.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 27, 2008
 * </p>
 */
public class ConnectionPoolSingleRetryTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(ConnectionPoolSingleRetryTestCase.class);
   protected static String DELAY = "delay";
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
   
   
   /**
    * Verifies that the "create new socket on last retry" feature does not apply when
    * maxPoolSize == 1.  Verifies that the same socket is reused multiple times.
    */
   public void testMaxPoolSizeOne() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - no connection checking.
      setupServer();
      
      // Create first client.
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("numberOfCallRetries", "1");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify numberOfCallRetries == 1.
      assertTrue(client.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(1, invoker.getNumberOfCallRetries());
      
      // Get connection pool.
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(1, pool.size());
      SocketWrapper socketWrapper = (SocketWrapper) pool.get(0);
      
      for (int i = 0; i < 10000; i++)
      {
         client.invoke("xyz");
      }
      
      log.info("made 10000 invocations");
      assertEquals(1, pool.size());
      assertEquals(socketWrapper, pool.get(0));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
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
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
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