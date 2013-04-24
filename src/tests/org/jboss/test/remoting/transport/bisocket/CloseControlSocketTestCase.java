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
package org.jboss.test.remoting.transport.bisocket;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.test.remoting.transport.bisocket.ServerTimerReuseTestCase.TestCallbackHandler;
import org.jboss.test.remoting.transport.bisocket.ServerTimerReuseTestCase.TestInvocationHandler;


/**
 * TimerReuseTestCase verifies that it is safe to add a callback BisocketClientInvoker
 * after the static Timer used by the PingTimerTasks has shut down because all previous
 * PingTimerTasks have ended.  
 *  
 * See JBREM-748.
 * 
 * The phenomenon described in JBREM-748 seems to be platform dependent.  It's not clear,
 * for example, how long it takes for a Timer to shut down after all of its tasks have
 * ended.  Therefore, there are multiple test methods that wait varying amounts of
 * time before installing a second InvokerCallbackHandler.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2897 $
 * <p>
 * Copyright May 31, 2007
 * </p>
 */
public class CloseControlSocketTestCase extends TestCase
{
   public static int port;
  
   private static Logger log = Logger.getLogger(CloseControlSocketTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private SampleInvocationHandler invocationHandler;


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
   
   
   public void testControlSocketClosedWhenDisconnected() throws Throwable
   {
     log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      
      // Get control socket.
      BisocketServerInvoker serverInvoker = (BisocketServerInvoker) connector.getServerInvoker();
      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      field = ServerInvokerCallbackHandler.class.getDeclaredField("callBackClient");
      field.setAccessible(true);
      Client callbackClient = (Client) field.get(serverInvokerCallbackHandler);
      assertNotNull(callbackClient);
      BisocketClientInvoker callbackClientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket controlSocket = (Socket) field.get(callbackClientInvoker);
      assertNotNull(controlSocket);
      assertFalse(controlSocket.isClosed());
      
      // Remove callback handler, which will shut down control connection.
      // Verify that control socket gets closed.
      client.removeListener(callbackHandler);
      assertTrue(controlSocket.isClosed());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }    

   
   /**
    Verifies that a control socket is closed when it is replaced.
    */
   public void testControlSocketClosedWhenReplaced() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      
      // Get current control socket.
      BisocketServerInvoker serverInvoker = (BisocketServerInvoker) connector.getServerInvoker();
      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      field = ServerInvokerCallbackHandler.class.getDeclaredField("callBackClient");
      field.setAccessible(true);
      Client callbackClient = (Client) field.get(serverInvokerCallbackHandler);
      assertNotNull(callbackClient);
      BisocketClientInvoker callbackClientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket oldControlSocket = (Socket) field.get(callbackClientInvoker);
      assertNotNull(oldControlSocket);
      assertFalse(oldControlSocket.isClosed());
      
      // Cause recreation of control connection on server.
      field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      Set connectors = (Set) callbackConnectors.values().iterator().next();
      assertEquals(1, connectors.size());
      Connector callbackConnector = (Connector) connectors.iterator().next();
      BisocketServerInvoker callbackInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(callbackInvoker);
      assertEquals(1, controlConnectionThreadMap.size());
      Thread controlConnectionThread = (Thread) controlConnectionThreadMap.values().iterator().next();
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class ControlConnectionThreadClass = null;
      
      for (int i = 0; i < classes.length; i++)
      {
         if (classes[i].toString().indexOf("ControlConnectionThread") != -1)
         {
            ControlConnectionThreadClass = classes[i];
            break;
         }
      }
      
      assertNotNull(ControlConnectionThreadClass);
      field = ControlConnectionThreadClass.getDeclaredField("listenerId");
      field.setAccessible(true);
      String listenerId = (String) field.get(controlConnectionThread);
      assertNotNull(listenerId);
      callbackInvoker.createControlConnection(listenerId, false);

      // Verify old control socket is closed and has been replaced.
      Thread.sleep(2000);
      assertTrue(oldControlSocket.isClosed());
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket newControlSocket = (Socket) field.get(callbackClientInvoker);
      assertNotNull(newControlSocket);
      assertNotSame(oldControlSocket, newControlSocket);
      assertFalse(newControlSocket.isClosed());
      
      // Shut down.
      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("unable to send callback", e);
         }
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int count;
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         count++;
      }
   }
}