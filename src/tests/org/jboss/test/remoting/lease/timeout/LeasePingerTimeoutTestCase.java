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
package org.jboss.test.remoting.lease.timeout;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.LeasePinger;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-956.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Apr 20, 2008
 * </p>
 */
public class LeasePingerTimeoutTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(LeasePingerTimeoutTestCase.class);
   
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
   
   
   /**
    * Verifies that LeasePinger uses standard "timeout" parameter in the absence
    * of a "leasePingerTimeout" parameter.
    */
   public void testDefaultTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      
      // Get LeasePinger.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      
      // Prevent server from answering PINGs.
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(1, clientpool.size());
      ServerThread st = (ServerThread) clientpool.getContents().iterator().next();
      
      while (clientpool.size() > 0)
      {
         st.evict();
         Thread.sleep(1000);
      }
      
      field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      List threadpool = (List) field.get(serverInvoker);
      threadpool.clear();
      assertEquals(0, threadpool.size());
      log.info("clientpool.size(): " + clientpool.size());
      log.info("threadpool.size(): " + threadpool.size());
      serverInvoker.setMaxPoolSize(0);
      
      // Verify that PING fails after default timeout.
      Field succeedField = LeasePinger.class.getDeclaredField("pingSucceeded");
      succeedField.setAccessible(true);
      Field invokedField = LeasePinger.class.getDeclaredField("pingInvoked");
      invokedField.setAccessible(true);
      boolean pingInvoked = ((Boolean) invokedField.get(pinger)).booleanValue();
      
      while (!pingInvoked)
      {
         Thread.sleep(1000);
         pingInvoked = ((Boolean) invokedField.get(pinger)).booleanValue();
      }
      
      // Verify that PING has been sent but not answered.
      log.info("pingInvoked: " + pingInvoked);
      Thread.sleep(5000);
      assertTrue(((Boolean) invokedField.get(pinger)).booleanValue());
      assertFalse(((Boolean) succeedField.get(pinger)).booleanValue());
      log.info("LeasePinger has not timed out after 5000 ms");
      
      client.setDisconnectTimeout(0);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that LeasePinger uses "leasePingerTimeout" if it is present.
    */
   public void testLeasePingerTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(LeasePinger.LEASE_PINGER_TIMEOUT, "1000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      
      // Get LeasePinger.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      
      // Prevent server from answering PINGs.
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(1, clientpool.size());
      ServerThread st = (ServerThread) clientpool.getContents().iterator().next();
      
      while (clientpool.size() > 0)
      {
         st.evict();
         Thread.sleep(1000);
      }
      
      field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      List threadpool = (List) field.get(serverInvoker);
      threadpool.clear();
      assertEquals(0, threadpool.size());
      log.info("clientpool.size(): " + clientpool.size());
      log.info("threadpool.size(): " + threadpool.size());
      serverInvoker.setMaxPoolSize(0);
      
      // Verify that PING fails after default timeout.
      Field succeedField = LeasePinger.class.getDeclaredField("pingSucceeded");
      succeedField.setAccessible(true);
      Field invokedField = LeasePinger.class.getDeclaredField("pingInvoked");
      invokedField.setAccessible(true);
      boolean pingInvoked = ((Boolean) invokedField.get(pinger)).booleanValue();
      
      while (!pingInvoked)
      {
         Thread.sleep(1000);
         pingInvoked = ((Boolean) invokedField.get(pinger)).booleanValue();
      }
      
      // Verify that PING has been sent but not answered.
      log.info("pingInvoked: " + pingInvoked);
      Thread.sleep(5000);
      log.info("ping invoked:   " + ((Boolean)invokedField.get(pinger)).booleanValue());
      log.info("ping succeeded: " + ((Boolean)succeedField.get(pinger)).booleanValue());
      assertTrue(((Boolean) invokedField.get(pinger)).booleanValue());
      assertFalse(((Boolean) succeedField.get(pinger)).booleanValue());
      log.info("LeasePinger has timed out within 5000 ms");
      
      client.setDisconnectTimeout(0);
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
      locatorURI += "/?enableLease=true";
      locatorURI += "&" + InvokerLocator.CLIENT_LEASE + "=true";
      locatorURI += "&leasePeriod=10000";
      locatorURI += "&timeout=10000";
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.addConnectionListener(new TestConnectionListener());
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
   
   
   static class TestConnectionListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {  
      }   
   }
}