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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketClientInvoker;


/**
 * Unit test for JBREM-845.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 11, 2007
 * </p>
 */
public class CallbackClientWithZeroTimeoutTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackClientWithZeroTimeoutTestCase.class);
   
   private static boolean firstTime = true;
   private static String CALLBACK = "callback";
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
   
   
   public void testCallbackSocketFailure() throws Throwable
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
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      log.info("callback handler is installed");
      
      // Make sure callback client invoker has timeout == 0.
      Client callbackClient = invocationHandler.callbackHandler.getCallbackClient();
      SocketClientInvoker callbackClientInvoker = (SocketClientInvoker) callbackClient.getInvoker();
      assertEquals(0, callbackClientInvoker.getTimeout());
      log.info("timeout == 0");
      
      // Verify failing PingTimerTask kicks thread out of BisocketClientInvoker.createSocket().
      // 1. Get client side control socket.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      BisocketServerInvoker callbackServerInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      Field field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(callbackServerInvoker);
      assertEquals(1, controlConnectionThreadMap.size());
      Thread controlConnectionThread = (Thread) controlConnectionThreadMap.values().iterator().next();
      assertNotNull(controlConnectionThread);
  
      Class controlConnectionThreadClass = null;
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         if (classes[i].getName().indexOf("ControlConnectionThread") >= 0)
         {
            controlConnectionThreadClass = classes[i];
            break;
         }
      }
      assertNotNull(controlConnectionThreadClass);
      field = controlConnectionThreadClass.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket controlSocket = (Socket) field.get(controlConnectionThread);
      assertNotNull(controlSocket);
      
      // 2. Get server side ControlConnectionThread and stop it.
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSocketThreads");
      field.setAccessible(true);
      Set secondaryServerSocketThreads = (Set) field.get(connector.getServerInvoker());
      Thread secondaryServerSocketThread = (Thread) secondaryServerSocketThreads.iterator().next();
      assertNotNull(secondaryServerSocketThread);
      secondaryServerSocketThread.stop();
      log.info("stopped secondaryServerSocketThread");
      
      // 3. Try to do callback.
      client.invokeOneway(CALLBACK);
      log.info("requested CALLBACK");
      
      // 4. Close client side control socket so PING fails.
      controlSocket.close();
      log.info("closed controlSocket");
      
      // 5. Test that attempt to create a socket for callback threw exception.
      Thread.sleep(5000);
      assertTrue(invocationHandler.ok);
      log.info("got expected Exception doing callback");

      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?timeout=0";
      locatorURI += "&" + Bisocket.PING_FREQUENCY + "=1000";
      locatorURI += "&" + Bisocket.MAX_RETRIES + "=1";
      locatorURI += "&numberOfCallRetries=1";
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
      public boolean ok;
      public ServerInvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = (ServerInvokerCallbackHandler) callbackHandler;
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         if (CALLBACK.equals(invocation.getParameter()))
         {
            try
            {
               log.info("calling handleCallback()");
               callbackHandler.handleCallback(new Callback(CALLBACK));
               log.info("called handleCallback()");
            }
            catch (HandleCallbackException e)
            {
               log.info("error: " + e.getMessage(), e);
               Throwable cause = e.getCause();
               log.info("cause: " + cause);
               cause = cause.getCause();
               log.info("cause: " + cause);
               if (cause instanceof IOException && "Unable to create socket".equals(cause.getMessage()))
               {
                  ok = true;
               }
            }
         }
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public boolean ok;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         ok = true;
         log.info("received callback");
      }  
   }
}