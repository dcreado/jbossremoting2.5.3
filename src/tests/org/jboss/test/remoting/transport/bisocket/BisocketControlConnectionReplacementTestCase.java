/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;

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
import org.jboss.remoting.callback.DefaultCallbackErrorHandler;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;


/**
 * Unit test for JBREM-1147.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Aug 14, 2009
 * </p>
 */
public class BisocketControlConnectionReplacementTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(BisocketControlConnectionReplacementTestCase.class);
   
   protected static int INITIAL_WRITES;
   protected static boolean firstTime = true;
   protected static int secondaryServerSocketPort;
   protected static int numberOfCallbacks = 10;
   protected static Object lock = new Object();
   protected static TestCallbackHandler testCallbackHandler;
   
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
         
         String jdkVersion = System.getProperty("java.version");
         log.info("jdk version: " + jdkVersion);
         if (jdkVersion != null && jdkVersion.indexOf("1.4") >= 0)
         {
            INITIAL_WRITES = 5;
         }
         else
         {
            INITIAL_WRITES = 2;
         }
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testCreateSocketWithReplacedControlConnection() throws Throwable
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
      testCallbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(testCallbackHandler, metadata);
      
      synchronized (lock)
      {
         lock.wait(120000);
      }
      
      assertEquals(numberOfCallbacks, testCallbackHandler.counter);
      assertEquals(numberOfCallbacks - 1, testCallbackHandler.max);
      
      client.removeListener(testCallbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected String getServerSocketName()
   {
      return TestServerSocketFactory.class.getName();
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?" + Bisocket.PING_FREQUENCY + "=2000";
      locatorURI += "&" + DefaultCallbackErrorHandler.CALLBACK_ERRORS_ALLOWED + "=100";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      secondaryServerSocketPort = PortUtil.findFreePort(host);
      config.put(Bisocket.SECONDARY_BIND_PORT, Integer.toString(secondaryServerSocketPort));
      config.put(ServerInvoker.SERVER_SOCKET_FACTORY, getServerSocketName());
      config.put("numberOfCallRetries", "5");
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
      int counter;
      
      public void addListener(final InvokerCallbackHandler callbackHandler)
      {
         if (counter++ > 0)
            return;
         
         new Thread()
         {
            public void run()
            {
               for (int i = 0; i < 10 * numberOfCallbacks; i++)
               {
                  try
                  {
                     if (testCallbackHandler.counter >= numberOfCallbacks)
                     {
                        return;
                     }
                     try
                     {
                        Thread.sleep(1000);
                     }
                     catch (InterruptedException e)
                     {
                        log.error("Unexpected interrupt", e);
                     }
                     log.info("sending callback: " + i);
                     callbackHandler.handleCallback(new Callback(Integer.toString(i)));
                     log.info("sent callback: " + i);
                  }
                  catch (HandleCallbackException e)
                  {
                     log.error("Callback error", e);
                  }
               }  
            }
         }.start();
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
      int counter;
      int max;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback: " + counter++);
         max = Math.max(Integer.valueOf((String) callback.getParameter()).intValue(), max);
         log.info("max: " + max);
         if (counter >= numberOfCallbacks)
         {
            synchronized (lock)
            {
               lock.notifyAll();
            }
         }
      }  
   }
   
   static public class TestServerSocketFactory extends ServerSocketFactory
   {
      int timeout;
      int initialWrites;
      
      public TestServerSocketFactory()
      {
         this.timeout = 5000;
         this.initialWrites = INITIAL_WRITES;
      }      
      public TestServerSocketFactory(int timeout, int initialWrites)
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public ServerSocket createServerSocket() throws IOException
      {
         ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket();
         log.info("returning: " + ss);
         return ss;
      }
      public ServerSocket createServerSocket(int port) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = ServerSocketFactory.getDefault().createServerSocket(port);
         }
         else
         {
            ss = new TestServerSocket(port, timeout, initialWrites);
         }
         log.info("returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = ServerSocketFactory.getDefault().createServerSocket(port, backlog);
         }
         else
         {
            ss = new TestServerSocket(port, backlog, timeout, initialWrites);
         }
         log.info("returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = ServerSocketFactory.getDefault().createServerSocket(port, backlog, ifAddress);
         }
         else
         {
            ss = new TestServerSocket(port, backlog, ifAddress, timeout, initialWrites);
         }
         log.info("returning: " + ss);
         return ss;
      }
   }
   
   
   static class TestServerSocket extends ServerSocket
   {
      int timeout;
      int initialWrites;

      public TestServerSocket(int timeout, int initialWrites) throws IOException
      {
         super();
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestServerSocket(int port, int timeout, int initialWrites) throws IOException
      {
         super(port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestServerSocket(int port, int backlog, int timeout, int initialWrites) throws IOException
      {
         super(port, backlog);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestServerSocket(int port, int backlog, InetAddress bindAddr, int timeout, int initialWrites) throws IOException
      {
         super(port, backlog, bindAddr);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public Socket accept() throws IOException
      {
         Socket s = new TestSocket(timeout, initialWrites);
         implAccept(s);
         return s;
      }
      public String toString()
      {
         return "TestServerSocket[" + getLocalPort() + "]";
      }
   }
   
   
   static class TestSocket extends Socket
   {
      int timeout;
      int initialWrites;
      
      public TestSocket(int timeout, int initialWrites)
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestSocket(String host, int port, int timeout, int initialWrites) throws UnknownHostException, IOException
      {
         super(host, port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestSocket(InetAddress address, int port, int timeout, int initialWrites) throws IOException
      {
         super(address, port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestSocket(String host, int port, InetAddress localAddr, int localPort, int timeout, int initialWrites) throws IOException
      {
         super(host, port, localAddr, localPort);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public TestSocket(InetAddress address, int port, InetAddress localAddr, int localPort, int timeout, int initialWrites) throws IOException
      {
         super(address, port, localAddr, localPort);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public OutputStream getOutputStream() throws IOException
      {
         return new TestOutputStream(super.getOutputStream(), timeout, initialWrites);
      }
      public String toString()
      {
         return "TestSocket[" + getLocalPort() + "->" + getPort() + "]";
      }
   }
 
   public static class TestOutputStream extends OutputStream
   {
      OutputStream os;
      int timeout;
      boolean closed;
      int initialWrites;
      boolean doCounterTest = true;
      int counter;
      
      public TestOutputStream(OutputStream os, int timeout, int initialWrites)
      {
         this.os = os;
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public void close()throws IOException
      {
         closed = true;
         super.close();
         log.info(this + " closed");
      }
      public void write(int b) throws IOException
      {
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new SocketException("closed");
         }
         if (doCounterTest && ++counter > initialWrites)
         {
            close();
            throw new SocketException("closed");
         }
         os.write(b);
      }
      public void write(byte b[], int off, int len) throws IOException
      {
         for (int i = 0; i < len; i++)
         {
            System.out.print(b[i] + " ");
         }
         System.out.println("");
         
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new SocketException("closed");
         }
         log.info("TestOutputStream: counter = " + counter + ", initialWrites = " + initialWrites);
         if (++counter > initialWrites)
         {
            close();
            throw new SocketException("closed");
         }
         try
         {
            log.info(this + " writing");
            doCounterTest = false;
            os.write(b, off, len);
            doCounterTest = true;
            log.info(this + " back from writing");
         }
         catch (IOException e)
         {
            log.info("exception: ", e);
            throw e;
         }
      }
   }
}