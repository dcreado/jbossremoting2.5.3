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
package org.jboss.test.remoting.transport.socket.oneway;

import java.net.InetAddress;
import java.util.HashMap;

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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;

/** 
 * Unit test to confirm that MicroSocketClientInvoker.transport()
 * returns socket to pool in the case of oneway invocations. See JBREM-684.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2525 $
 * <p>
 * Copyright Jan 22, 2007
 * </p>
 */
public class OnewayInvocationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger("OnewayInvocationTestCase");
   private static final int MAX_POOL_SIZE = 50;
   private static boolean firstTime = true;

   
   public void setUp()
   {
      if (firstTime)
      {
         firstTime = false;
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender); 
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      }
   }
   
   
   public void testServerSideThreads() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, Integer.toString(MAX_POOL_SIZE));
      Client client = new Client(locator, clientConfig);
      client.connect();
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) client.getInvoker();
      invoker.setNumberOfRetries(3);
      
      int i = 0;
      try
      {
         for (; i < MAX_POOL_SIZE + 10; i++)
         {
            client.invokeOneway(new Integer(i), null, false);
            log.debug("invocation: " + i);
         }
      }
      catch (Throwable t)
      {
         log.info("failed on invocation: " + i + " (should be " + MAX_POOL_SIZE + ")");
      }

      Thread.sleep(2000);
      assertEquals(MAX_POOL_SIZE + 10, i);
      assertEquals(MAX_POOL_SIZE + 10, handler.counter);
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }


   public void testClientSideThreads() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, Integer.toString(MAX_POOL_SIZE));
      Client client = new Client(locator, clientConfig);
      client.connect();
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) client.getInvoker();
      invoker.setNumberOfRetries(3);
      
      int i = 0;
      try
      {
         for (; i < MAX_POOL_SIZE + 10; i++)
         {
            client.invokeOneway(new Integer(i), null, true);
            log.debug("invocation: " + i);
         }
      }
      catch (Throwable t)
      {
         log.info("failed on invocation: " + i + " (should be " + MAX_POOL_SIZE + ")");
      }

      Thread.sleep(2000);
      assertEquals(MAX_POOL_SIZE + 10, i);
      assertEquals(MAX_POOL_SIZE + 10, handler.counter);
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }

   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   public static class TestHandler implements ServerInvocationHandler
   {
      public int counter;
      private Object lock = new Object();
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         synchronized(lock)
         {
            counter++;
         }
         Integer i = (Integer) invocation.getParameter();
         log.info("got invocation: " + i.intValue());
         return new Integer(i.intValue() + 1);
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("error handling callback");
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}
