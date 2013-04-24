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
package org.jboss.test.remoting.transport.socket.timeout;

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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.SocketClientInvoker;

/** 
 * Unit test written for JBREM-690: http://jira.jboss.com/jira/browse/JBREM-690.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2907 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public class CallbackTimeoutTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(CallbackTimeoutTestCase.class);
   protected static boolean firstTime = true;
   
   protected static final String INVOKE = "invoke";
   protected static final String CALLBACK = "callback";
   protected static final String CALLBACK_ONEWAY = "callbackOneway";
   
   
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
   

   public void xtestCallbackTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(serverConfig);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(locator, clientConfig);
      client.connect();
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      
      Object response = client.invoke(INVOKE);
      assertEquals(INVOKE, response);
      log.info("invocation succeeded");

      client.invoke(CALLBACK);
      assertTrue(callbackHandler.receivedCallback);
      log.info("received first callback");
      
      Thread.sleep(ServerInvoker.DEFAULT_TIMEOUT_PERIOD + 5000);

      callbackHandler.receivedCallback = false;
      client.invoke(CALLBACK);
      assertTrue(callbackHandler.receivedCallback);
      log.info("received second callback");
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   

   public void testOnewayCallbackTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      
      // Connection checking is no longer necessary since ClientSocketWrapper
      // implements OpenConnectionChecker.
//      serverConfig.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraServerConfig(serverConfig);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      clientConfig.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      clientConfig.put(SocketClientInvoker.SO_TIMEOUT_FLAG, "5000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(locator, clientConfig);
      client.connect();
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      
      Object response = client.invoke(INVOKE);
      assertEquals(INVOKE, response);
      log.info("invocation succeeded");

      client.invoke(CALLBACK_ONEWAY);
      Thread.sleep(2000);
      assertTrue(callbackHandler.receivedCallback);
      log.info("received first callback");
      
      Thread.sleep(ServerInvoker.DEFAULT_TIMEOUT_PERIOD + 5000);

      log.info("waking up");
      callbackHandler.receivedCallback = false;
      client.invoke(CALLBACK_ONEWAY);
      log.info("sent second invocation");
      Thread.sleep(2000);
      assertTrue(callbackHandler.receivedCallback);
      log.info("received second callback");
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config)
   {  
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler callbackHandler;
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         if (INVOKE.equals(command))
         {
            return INVOKE;
         }
         else if (CALLBACK.equals(command))
         {
            try
            {
               callbackHandler.handleCallback(new Callback("callback"));
               return null;
            }
            catch (HandleCallbackException e)
            {
               log.error("error handling callback");
               throw e;
            }
         }
         else if (CALLBACK_ONEWAY.equals(command))
         {
            try
            {
               ServerInvokerCallbackHandler sich = (ServerInvokerCallbackHandler) callbackHandler;
               sich.handleCallbackOneway(new Callback("callback"), true);
               return null;
            }
            catch (HandleCallbackException e)
            {
               log.error("error handling callback");
               throw e;
            }
         }
         else
         {
            log.error("unrecognized command: " + command);
            throw new Exception("unrecognized command: " + command);
         }
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = callbackHandler;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
   
   
   public class TestCallbackHandler implements InvokerCallbackHandler
   {
      public boolean receivedCallback;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         receivedCallback = true;
         log.info("received callback: " + callback);
      }  
   }
}