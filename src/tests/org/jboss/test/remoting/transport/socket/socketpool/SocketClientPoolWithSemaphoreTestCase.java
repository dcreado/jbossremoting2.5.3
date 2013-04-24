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
package org.jboss.test.remoting.transport.socket.socketpool;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-845.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Nov 30, 2007
 * </p>
 */
public class SocketClientPoolWithSemaphoreTestCase extends TestCase
{
   protected static String WAIT = "wait";
   protected static String DURATION = "duration";
   
   private static Logger log = Logger.getLogger(SocketClientPoolWithSemaphoreTestCase.class);
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
   
   
   public void testStressClientPoolTen() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "10");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      for (int i = 0; i < 5000; i++)
      {
         assertEquals(Integer.toString(i), (String) client.invoke(Integer.toString(i)));
      }
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testStressClientPoolOne() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "1");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(1, clientInvoker.getNumberOfAvailableConnections());
      
      for (int i = 0; i < 5000; i++)
      {
         assertEquals(Integer.toString(i), (String) client.invoke(Integer.toString(i)));
      }
      
      assertEquals(1, clientInvoker.getNumberOfAvailableConnections());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testMaxPoolSize() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "10");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      final HashMap metadata = new HashMap();
      metadata.put(DURATION, "20000");
      
      for (int i = 0; i < 20; i++)
      {
         new Thread()
         {
            public void run()
            {
               try
               {
                  client.invoke(WAIT, metadata);
               }
               catch (Throwable e)
               {
                  log.error("Error", e);
               }
            }
         }.start();
      }
      
      Thread.sleep(10000);
      assertEquals(10, invocationHandler.counter);
      assertEquals(0, clientInvoker.getNumberOfAvailableConnections());
      
      Thread.sleep(20000);
      assertEquals(20, invocationHandler.counter);
      assertEquals(0, clientInvoker.getNumberOfAvailableConnections());
      
      Thread.sleep(20000);
      assertEquals(20, invocationHandler.counter);
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testRestartServer() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "10");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      for (int i = 0; i < 50; i++)
      {
         assertEquals(Integer.toString(i), (String) client.invoke(Integer.toString(i)));
      }
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
     
      disableServer(connector);
      shutdownServer();
      setupServer(port);
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      for (int i = 0; i < 50; i++)
      {
         assertEquals(Integer.toString(i), (String) client.invoke(Integer.toString(i)));
      }
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSemaphoreReleaseAfterGetConnection() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "/?" + MicroSocketClientInvoker.CLIENT_SOCKET_CLASS_FLAG + "=bogus";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      log.info("clientLocator: " + clientLocator);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "10");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      try
      {
         log.info(client.invoke("abc"));
         fail("expected exception");
      }
      catch (Exception e)
      {
         log.info("got expected exception");
      }
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSemaphoreReleaseAfterSocketException() throws Throwable
   {
      String exceptionClass = SocketWrapperWithSocketException.class.getName();
      doSemaphoreReleaseAfterSocketException(exceptionClass);
   }
   
   
   
   public void testSemaphoreReleaseAfterIOException() throws Throwable
   {
      String exceptionClass = SocketWrapperWithIOException.class.getName();
      doSemaphoreReleaseAfterSocketException(exceptionClass);
   }
   
   
   protected void doSemaphoreReleaseAfterSocketException(String exceptionClass) throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "/?" + MicroSocketClientInvoker.CLIENT_SOCKET_CLASS_FLAG + "=" + exceptionClass;
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      log.info("clientLocator: " + clientLocator);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "10");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) client.getInvoker();
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
      try
      {
         log.info(client.invoke("abc"));
         fail("expected exception");
      }
      catch (Exception e)
      {
         log.info("got expected exception: " + e, e);
      }
      
      assertEquals(10, clientInvoker.getNumberOfAvailableConnections());
      
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
      setupServer(-1);
   }
   
   
   protected void setupServer(int localPort) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      
      if (localPort == -1)
         port = PortUtil.findFreePort(host);
      else
         port = localPort;
         
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

      for (int i = 0; i < 5; i++)
      {
    	  try
    	  {
    		  connector.start();
    	  }
    	  catch (Exception e)
    	  {
    		  log.error("unable to start Connector");
    		  Thread.sleep(60000);
    	  }
      }
   }
   
   
   protected void disableServer(Connector connector) throws Exception
   {
      log.info("disabling " + connector);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(serverInvoker);
      ServerSocket ss = (ServerSocket) serverSockets.get(0);
      ss.close();
      
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      Iterator it = clientpool.getContents().iterator();
      field = ServerThread.class.getDeclaredField("socket");
      field.setAccessible(true);
      
      while (it.hasNext())
      {
         Socket socket = (Socket) field.get(it.next());
         socket.close();
      }
      
      field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      List threadpool = new ArrayList((List) field.get(serverInvoker));
      it = threadpool.iterator();
      field = ServerThread.class.getDeclaredField("socket");
      field.setAccessible(true);
      
      while (it.hasNext())
      {
         Socket socket = (Socket) field.get(it.next());
         socket.close();
      }
      
      log.info("disabled " + connector);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public int counter;
      private Object lock = new Object();
      
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         synchronized (lock)
         {
            counter++;
         }
         
         String command =  (String) invocation.getParameter();
         
         if (WAIT.equals(command))
         {
            Map metadata = invocation.getRequestPayload();
            int duration = Integer.parseInt((String) metadata.get(DURATION));
            Thread.sleep(duration);
         }
         
         return command;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}