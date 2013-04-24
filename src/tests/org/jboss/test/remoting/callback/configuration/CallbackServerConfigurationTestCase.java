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
package org.jboss.test.remoting.callback.configuration;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/** 
 * This unit test verifies that InvokerLocator parameters are passed to callback
 * server invokers created by Client.addListener().
 * 
 * See JBREM-733.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2343 $
 * <p>
 * Copyright Apr 11, 2007
 * </p>
 */
public class CallbackServerConfigurationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackServerConfigurationTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   
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
   
   
   public void testCallbackConfiguration() throws Throwable
   {      
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port + "/?timeout=12345"; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(serverConfig);
      connector = new Connector(serverLocator, serverConfig);
      connector.create();
      ServerInvocationHandler invocationHandler = new SimpleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      System.out.println("Started remoting server with locator uri of: " + locatorURI);
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      log.info("client added callback handler");

      // Test connection.
      Integer response = (Integer) client.invoke(new Integer(17));
      assertEquals(18, response.intValue());
      
      // Test callback server invoker configuration.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectorsMap = (Map) field.get(client);
      assertEquals(1, callbackConnectorsMap.size());
      Set callbackConnectorsSet = (Set) callbackConnectorsMap.values().iterator().next();
      assertEquals(1, callbackConnectorsSet.size());
      Connector callbackConnector = (Connector) callbackConnectorsSet.iterator().next();
      ServerInvoker callbackServerInvoker = callbackConnector.getServerInvoker();
      assertEquals(12345, callbackServerInvoker.getTimeout());
 
      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
      connector.destroy();
   }

   public void tearDown()
   {
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
   
   
   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   static class SimpleInvocationHandler implements ServerInvocationHandler
   {  
      public void addListener(InvokerCallbackHandler callbackHandler) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Integer i = (Integer) invocation.getParameter();
         return new Integer(i.intValue() + 1);
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class SimpleCallbackHandler implements InvokerCallbackHandler
   {  
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.debug("received callback: " + callback.getParameter());
      }
   }
}
