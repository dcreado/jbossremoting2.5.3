/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLProtocolException;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationFailureException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;


/**
 * Unit tests for JBREM-1120.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Apr 22, 2009
 * </p>
 */
public abstract class WriteTimeoutTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(WriteTimeoutTestParent.class);
   
   private static boolean firstTime = true;
   protected static int secondaryServerSocketPort;
   protected static boolean callbackTest;
   
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
      
      TestOutputStream.counter = 0;
      callbackTest = false;
   }

   
   public void tearDown()
   {
   }
   
   
   public void testClientWriteTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, false, "", -1, -1);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(SocketWrapper.WRITE_TIMEOUT, "1000");
      SocketFactory sf = (SocketFactory) getSocketFactoryConstructor().newInstance(new Object[]{new Integer(5000), new Integer(1)});
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf);
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      log.info("**************************************");
      log.info("*** WorkerThread error is expected ***");
      log.info("**************************************");
      
      // Test client side write timeout.
      try
      {
         client.invoke("abc");
      }
      catch (InvocationFailureException e)
      {
         log.info(e.getMessage());
         assertNotNull(e.getMessage());
         assertTrue(e.getMessage().startsWith("Unable to perform invocation"));
         assertTrue(e.getCause() instanceof IOException);
         IOException ioe = (IOException) e.getCause();
         assertEquals("closed", ioe.getMessage());
         log.info("got expected Exception");
      }
      catch (Throwable t)
      {
         log.error("got unexpected Exception", t);
         fail("got unexpected Exception");
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testServerWriteTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, false, "1000", 5000, 1);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("numberOfCallRetries", "1");
      clientConfig.put("timeout", "10000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      log.info("**************************************");
      log.info("*** WorkerThread error is expected ***");
      log.info("**************************************");
      
      // Test server side write timeout.
      try
      {
         client.invoke("abc");
      }
      catch (InvocationFailureException e)
      {
         log.info(e.getMessage());
//         assertNotNull(e.getMessage());
//         assertTrue(e.getMessage().startsWith("Unable to perform invocation"));
//         assertTrue(e.getCause() instanceof EOFException);
         log.info("got expected Exception");
      }
      catch (Throwable t)
      {
         log.error("got unexpected Exception", t);
         fail("got unexpected Exception");
      }
      
      // Test server invoker state.
      Thread.sleep(4000);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      assertEquals(0, serverInvoker.getCurrentClientPoolSize());
      assertEquals(1, serverInvoker.getCurrentThreadPoolSize());
      log.info("used ServerThread has returned to threadPool");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testClientCallbackWriteTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      if (isBisocket(getTransport()))
      {
         callbackTest = true;
      }
      setupServer(false, false, "", -1, -1);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(SocketWrapper.WRITE_TIMEOUT, "1000");
      if (isBisocket(getTransport()))
      {
         SocketFactory sf = (SocketFactory) getSocketFactoryConstructor().newInstance(new Object[]{new Integer(5000), new Integer(1)});
         clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf);
      }
      else
      {
         ServerSocketFactory ssf = (ServerSocketFactory) getServerSocketFactoryConstructor().newInstance(new Object[]{new Integer(5000), new Integer(-1)});
         clientConfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      }
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection
      client.invoke("abc");
      log.info("connection is good");
      
      // Test client callback write timeout.
      log.info("registering callback handler");
      log.info("**************************************");
      log.info("*** WorkerThread error is expected ***");
      log.info("**************************************");
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      if (isBisocket(getTransport()))
      {
//         metadata.put(SocketWrapper.WRITE_TIMEOUT, "1000");
         metadata.put(Remoting.SOCKET_FACTORY_NAME, getSocketFactoryClassName());
         metadata.put("numberOfCallRetries", "1");
         metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
         metadata.put(Bisocket.PING_FREQUENCY, "11111111"); 
      }
      else
      {
//         metadata.put(SocketWrapper.WRITE_TIMEOUT, "1000");
         metadata.put(ServerInvoker.SERVER_SOCKET_FACTORY, getServerSocketFactoryClassName());
         metadata.put("numberOfCallRetries", "1");
      }
      client.addListener(callbackHandler, metadata, null, true);
      log.info("called Client.addListener()");
      
      // Test server invoker state.
      // Wait for local ServerThread to time out.  Might take a while in bisocket transports, since
      // the request to get a socket for the callback client invoker needs its own write on the
      // control socket.
      Thread.sleep(20000);
      log.info("back from sleep");
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      SocketServerInvoker serverInvoker = (SocketServerInvoker) callbackConnector.getServerInvoker();
      assertEquals(0, serverInvoker.getCurrentClientPoolSize());
      assertEquals(1, serverInvoker.getCurrentThreadPoolSize());
      log.info("used ServerThread has returned to threadPool");

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testServerCallbackWriteTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      if (isBisocket(getTransport()))
      {
         callbackTest = true;
         setupServer(true, false, "1000", 5000, 1);
      }
      else
      {
         setupServer(false, true, "1000", 5000, 1);
      }
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("numberOfCallRetries", "1");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection
      client.invoke("abc");
      log.info("connection is good");
      
      // Test server callback write timeout.
      log.info("registering callback handler");
      log.info("**************************************");
      log.info("*** WorkerThread error is expected ***");
      log.info("**************************************");
      HashMap metadata = new HashMap();
      if (isBisocket(getTransport()))
      {
         metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      }
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, metadata, null, true);
      log.info("added listener");
      
      // Test server invoker state.
      Thread.sleep(20000);
      log.info("waking up");
      Throwable t = invocationHandler.t;
      assertTrue(t instanceof HandleCallbackException);
      log.info("t.getCause:", t.getCause());
      if (t.getCause() instanceof InvocationFailureException)
      {
         InvocationFailureException e = (InvocationFailureException) t.getCause();
         assertNotNull(e.getMessage());
         assertTrue(e.getMessage().startsWith("Unable to perform invocation"));
         assertTrue(e.getCause() instanceof IOException);
         IOException ioe = (IOException) e.getCause();
         assertEquals("closed", ioe.getMessage());
      }
      else
      {
         assertTrue(t.getCause() instanceof CannotConnectException);
         log.info("t.getCause().getCause(): ", t.getCause().getCause());
         assertTrue(t.getCause().getCause() instanceof InvocationTargetException);
         log.info("t.getCause().getCause().getCause(): ", t.getCause().getCause().getCause());
//         assertTrue(t.getCause().getCause().getCause() instanceof SSLProtocolException);
         assertTrue(t.getCause().getCause().getCause() instanceof IOException);
         assertEquals("closed", t.getCause().getCause().getCause().getMessage());
      }
      log.info("got expected Exception");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();
   
   protected boolean isBisocket(String transport)
   {
      return transport.indexOf("bisocket") >= 0;
   }
   
   protected String getServerSocketFactoryClassName()
   {
      return TestServerSocketFactory.class.getName();
   }
   
   protected Constructor getServerSocketFactoryConstructor() throws NoSuchMethodException
   {
      return TestServerSocketFactory.class.getConstructor(new Class[]{int.class, int.class});
   }
   
   protected String getSocketFactoryClassName()
   {
      return TestSocketFactory.class.getName();
   }
   
   protected Constructor getSocketFactoryConstructor() throws NoSuchMethodException
   {
      return TestSocketFactory.class.getConstructor(new Class[]{int.class, int.class});
   }
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean setWriteTimeout, boolean setCallbackWriteTimeout,
                              String writeTimeout, int blockingTime, int initialWrites) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      if (isBisocket(getTransport()))
      {
         secondaryServerSocketPort = PortUtil.findFreePort(host);
         config.put(Bisocket.SECONDARY_BIND_PORT, Integer.toString(secondaryServerSocketPort));
         config.put(Bisocket.PING_FREQUENCY, "11111111");         
      }
      if (setWriteTimeout)
      {
         config.put(SocketWrapper.WRITE_TIMEOUT, writeTimeout);
         ServerSocketFactory ssf = (ServerSocketFactory) getServerSocketFactoryConstructor().newInstance(new Object[]{new Integer(blockingTime), new Integer(initialWrites)});
         config.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      }
      if (setCallbackWriteTimeout)
      {
         config.put(SocketWrapper.WRITE_TIMEOUT, writeTimeout);
         SocketFactory sf = (SocketFactory) getSocketFactoryConstructor().newInstance(new Object[]{new Integer(blockingTime), new Integer(initialWrites)});
         config.put(Remoting.CUSTOM_SOCKET_FACTORY, sf);
      }
      if (callbackTest)
      {
         config.put("numberOfCallRetries", "1");
      }
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
      Throwable t;

      public void addListener(final InvokerCallbackHandler callbackHandler)
      {
         new Thread()
         {
            public void run()
            {
               try
               {
                  log.info("sending callback");
                  callbackHandler.handleCallback(new Callback("callback"));
               }
               catch (Throwable t)
               {
                  log.info("throwable: ", t);
                  TestInvocationHandler.this.t = t;
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
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
   
   
   static public class TestServerSocketFactory extends ServerSocketFactory
   {
      int timeout;
      int initialWrites;
      
      public TestServerSocketFactory()
      {
         this.timeout = 5000;
         this.initialWrites = -1;
      }      
      public TestServerSocketFactory(int timeout, int initialWrites)
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public ServerSocket createServerSocket() throws IOException
      {
         ServerSocket ss = null;
         if (callbackTest)
         {
            ss = ServerSocketFactory.getDefault().createServerSocket();
         }
         else
         {
            ss = new TestServerSocket(timeout, initialWrites);
         }
         log.info("returning: " + ss);
         return ss;
      }
      public ServerSocket createServerSocket(int port) throws IOException
      {
         ServerSocket ss = null;
         if (callbackTest && port != secondaryServerSocketPort)
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
         if (callbackTest && port != secondaryServerSocketPort)
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
         if (callbackTest && port != secondaryServerSocketPort)
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
   
   
   public static class TestSocketFactory extends SocketFactory
   {
      int timeout;
      int initialWrites = -1;
      
      public TestSocketFactory()
      {
         timeout = 5000;
      }
      public TestSocketFactory(int timeout, int initialWrites)
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
      }
      public Socket createSocket()
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest)
         {
            s = new Socket();
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
         }
         log.info(this + " returning " + s);
         return s;
      }
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         log.info("callbackTest: " + callbackTest + ", port: " + arg1);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = new Socket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(arg0, arg1, timeout, initialWrites);
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         log.info("callbackTest: " + callbackTest + ", port: " + arg1);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = new Socket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(arg0, arg1, timeout, initialWrites);
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         log.info("callbackTest: " + callbackTest + ", port: " + arg1);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = new Socket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(arg0, arg1, arg2, arg3, timeout, initialWrites);
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         log.info("callbackTest: " + callbackTest + ", port: " + arg1);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = new Socket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(arg0, arg1, arg2, arg3, timeout, initialWrites);
         }
         log.info(this + " returning " + s);
         return s;
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
 
   static class TestOutputStream extends OutputStream
   {
      OutputStream os;
      int timeout;
      boolean closed;
      int initialWrites;
      boolean doWait = true;
      public static int counter;
      
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
         System.out.print("b: " + b);
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new IOException("closed");
         }
         if (doWait && ++counter > initialWrites)
         {
            try
            {
               log.info("TestOutputStream.write() sleeping: " + timeout);
               Thread.sleep(timeout);
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }
         }
         os.write(b);
      }
      public void write(byte b[], int off, int len) throws IOException
      {
         System.out.print("b: ");
         for (int i = 0; i < len; i++)
         {
            System.out.print(b[i] + " ");
         }
         System.out.println("");
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new IOException("closed");
         }
         log.info("TestOutputStream: counter = " + counter + ", initialWrites = " + initialWrites);
         if (++counter > initialWrites)
         {
            try
            {
               log.info("TestOutputStream.write() sleeping: " + timeout);
               Thread.sleep(timeout);
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }
         }
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new IOException("closed");
         }
         try
         {
            log.info(this + " calling write()");
            doWait = false;
            os.write(b, off, len);
            os.flush();
            doWait = true;
            log.info(this + " back from write()");
         }
         catch (IOException e)
         {
            log.info("exception: ", e);
            throw e;
         }
      }
   }
}