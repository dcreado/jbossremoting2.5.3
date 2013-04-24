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
package org.jboss.test.remoting.socketfactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.socketfactory.CreationListenerServerSocketFactory;
import org.jboss.remoting.socketfactory.CreationListenerSocketFactory;
import org.jboss.remoting.socketfactory.SocketCreationListener;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2895 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public abstract class CreationListenerTestRoot extends TestCase
{
   protected static Logger log = Logger.getLogger(CreationListenerTestRoot.class);
   protected static boolean firstTime = true;
   
   
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
   
   
   public void testSimpleClientSocketCases() throws Exception
   {
      log.info("entering " + getName());
      final InetAddress address = InetAddress.getLocalHost();
      final String host = address.getHostAddress();
      final int port = PortUtil.findFreePort(host);
      
      class T extends Thread
      {
         boolean running;
         boolean failed = false;
         void shutdown() {running = false;}
         boolean failed() {return failed;}
         public void run()
         {
            running = true;
            try
            {
               ServerSocket ss = new ServerSocket(port, 100, address);
               while (running)
               {
                  ss.accept();
               }
               ss.close();
            }
            catch (IOException e)
            {
               failed = true;
            }
         }
      };
      T t = new T();
      t.start();
      Thread.sleep(2000);

      TestListener listener = new TestListener();
      SocketFactory sf = getSocketFactory();
      SocketFactory clsf = new CreationListenerSocketFactory(sf, listener);
      
      assertFalse(listener.visited());
      clsf.createSocket();
      assertTrue(listener.visited());
      
      listener.reset();
      assertFalse(listener.visited());
      clsf.createSocket(host, port);
      assertTrue(listener.visited());
      
      listener.reset();
      assertFalse(listener.visited());
      clsf.createSocket(host, port, address, PortUtil.findFreePort(host));
      assertTrue(listener.visited());
      
      listener.reset();
      assertFalse(listener.visited());
      clsf.createSocket(address, port);
      assertTrue(listener.visited());
      
      listener.reset();
      assertFalse(listener.visited());
      clsf.createSocket(address, port, address, PortUtil.findFreePort(host));
      assertTrue(listener.visited());
      
      
      assertTrue(!t.failed());
      t.shutdown();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSimpleServerSocketCases() throws Exception
   {
      log.info("entering " + getName());
      final InetAddress address = InetAddress.getLocalHost();
      final String host = address.getHostAddress();
      
      class T extends Thread
      {
         private ServerSocket ss;
         private boolean failed = false;
         boolean failed() {return failed;}
         T(ServerSocket ss) {this.ss = ss;}
         public void run()
         {
            try
            {
               ss.accept();
               ss.close();
            }
            catch (IOException e)
            {
               log.error(e);
               failed = true;
            }
         }
      }
      
      TestListener listener = new TestListener();
      ServerSocketFactory ssf = getServerSocketFactory();
      ServerSocketFactory clssf = new CreationListenerServerSocketFactory(ssf, listener);
      
      ServerSocket ss = clssf.createServerSocket();
      int freePort = PortUtil.findFreePort(host);
      ss.bind(new InetSocketAddress(host, freePort));
      T t = new T(ss);
      t.start();
      assertFalse(listener.visited());
      new Socket(host, freePort);
      Thread.sleep(500);
      assertTrue(listener.visited());
      assertTrue(!t.failed());

      freePort = PortUtil.findFreePort(host);
      ss = clssf.createServerSocket(freePort);
      t = new T(ss);
      t.start();
      listener.reset();
      assertFalse(listener.visited());
      new Socket(host, freePort);
      Thread.sleep(500);
      assertTrue(listener.visited());
      assertTrue(!t.failed());
      
      freePort = PortUtil.findFreePort(host);
      ss = clssf.createServerSocket(freePort, 100);
      t = new T(ss);
      t.start();
      listener.reset();
      assertFalse(listener.visited());
      new Socket(host, freePort);
      Thread.sleep(500);
      assertTrue(listener.visited());
      assertTrue(!t.failed());
      
      freePort = PortUtil.findFreePort(host);
      ss = clssf.createServerSocket(freePort, 100, InetAddress.getLocalHost());
      t = new T(ss);
      t.start();
      listener.reset();
      assertFalse(listener.visited());
      new Socket(host, freePort);
      Thread.sleep(500);
      assertTrue(listener.visited());
      assertTrue(!t.failed());
      log.info(getName() + " PASSES");
   }
   
   
   public void testClientSideListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      TestListener listener1 = new TestListener();
      config.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listener1);
      TestListener listener2 = new TestListener();
      config.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listener2);
      Client client = new Client(locator, config);
      assertFalse(listener1.visited());
      assertFalse(listener2.visited());
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(17));
      assertEquals(18, i.intValue());
      Thread.sleep(500);
      assertTrue(listener1.visited());
      assertFalse(listener2.visited());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }

   
   public void testServerSideListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      ServerSocketFactory ssf = getServerSocketFactory();
      serverConfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      TestListener listener1 = new TestListener();
      serverConfig.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listener1);
      TestListener listener2 = new TestListener();
      serverConfig.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listener2);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(29));
      assertEquals(30, i.intValue());
      Thread.sleep(500);
      assertTrue(listener1.visited());
      assertFalse(listener2.visited());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testCallbackListeners() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      ServerSocketFactory ssf = getServerSocketFactory();
      serverConfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      TestListener listener1 = new TestListener();
      log.info("listener1: " + listener1);
      serverConfig.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listener1);
      TestListener listener2 = new TestListener();
      log.info("listener2: " + listener2);
      serverConfig.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listener2);
      addExtraServerConfig(serverConfig);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      TestListener listener3 = new TestListener();
      log.info("listener3: " + listener3);
      clientConfig.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listener3);
      TestListener listener4 = new TestListener();
      log.info("listener4: " + listener4);
      clientConfig.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listener4);
      addExtraClientConfig(clientConfig);
      Client client = new Client(locator, clientConfig);
      client.connect();
      client.addListener(new TestCallbackHandler(), null, null, true);
      Integer i = (Integer) client.invoke(new Integer(29));
      assertEquals(30, i.intValue());
      Thread.sleep(500);
      assertTrue(checkListenersVisited(listener1, listener2, listener3, listener4));
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected boolean checkListenersVisited(TestListener listener1, TestListener listener2,
         TestListener listener3, TestListener listener4)
   {
      return  listener1.visited() &&
              listener2.visited() &&
              listener3.visited() &&
              listener4.visited() &&
             !listener1.isClient() &&
              listener2.isClient() &&
              listener3.isClient() &&
             !listener4.isClient();
   }
   
   public void testExceptionFromClientListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      FailingTestListener listener = new FailingTestListener();
      config.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listener);
      Client client = new Client(locator, config);
      
      try
      {
         
         client.connect();
         client.invoke(new Integer(17));
         fail("didn't get expected exception");
      }
      catch (Exception e)
      {
         log.info("CAUGHT EXPECTED EXCEPTION");
      }
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testExceptionFromServerListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      ServerSocketFactory ssf = getServerSocketFactory();
      serverConfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      FailingTestListener listener = new FailingTestListener();
      serverConfig.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listener);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(ServerInvoker.TIMEOUT, "1000");
      final Client client = new Client(locator, clientConfig);
      
      class TestThread extends Thread
      {
         public boolean failed = false;
         
         public void run()
         {
            try
            {
               client.connect();
               client.invoke(new Integer(29));
               failed = true;
               client.disconnect();
               fail("invoke() should have timed out");
            }
            catch (Throwable t)
            {
               log.info("CAUGHT EXPECTED EXCEPTION");
            }
         }
      };

      TestThread t = new TestThread();
      t.start();
      Thread.sleep(2000);
      assertFalse(t.failed);
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testCreateClientListenerFromClassName() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      StaticTestListener.reset();
      String listenerClassName = StaticTestListener.class.getName();
      config.put(Remoting.SOCKET_CREATION_CLIENT_LISTENER, listenerClassName);
      Client client = new Client(locator, config);
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(17));
      assertEquals(18, i.intValue());
      Thread.sleep(500);
      assertTrue(StaticTestListener.visited());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testServerSideListenerFromClassName() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      ServerSocketFactory ssf = getServerSocketFactory();
      serverConfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      StaticTestListener.reset();
      String listenerClassName = StaticTestListener.class.getName();
      serverConfig.put(Remoting.SOCKET_CREATION_SERVER_LISTENER, listenerClassName);
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      Integer i = (Integer) client.invoke(new Integer(29));
      assertEquals(30, i.intValue());
      Thread.sleep(500);
      assertTrue(StaticTestListener.visited());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();
   
   
   protected SocketFactory getSocketFactory()
   {
      return SocketFactory.getDefault();
   }
   
   
   protected ServerSocketFactory getServerSocketFactory()
   {
      return ServerSocketFactory.getDefault();
   }
   
   
   protected void addExtraClientConfig(Map config)
   {  
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Integer i = (Integer) invocation.getParameter();
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
   
   
   public class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback: " + callback);
      }  
   }
}