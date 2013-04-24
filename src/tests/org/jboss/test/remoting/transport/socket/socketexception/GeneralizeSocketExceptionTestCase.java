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

package org.jboss.test.remoting.transport.socket.socketexception;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.MarshalException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit tests for JBREM-1146.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Sep 2, 2009
 * </p>
 */
public class GeneralizeSocketExceptionTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(GeneralizeSocketExceptionTestCase.class);
   private static String connectionResetMessage = "yada yada connection reset yada yada";
   private static String connectionClosedMessage = "yada yada connection closed yada yada";
   private static String brokenPipeMessage = "yada yada broken pipe yada yada";
   
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
      
      TestOutputStream.counter = 0;
   }

   
   public void tearDown()
   {
   }
   
   
   public void testConnectionResetNotGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(connectionResetMessage), 2));
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation fails.
      try
      {
         client.invoke("abc");
      }
      catch (MarshalException e)
      {
         Throwable t = e.getCause();
         assertEquals(connectionResetMessage, t.getMessage());
      }
      catch (Throwable e)
      {
         fail("Expecteded message: " + connectionResetMessage + ", got: " + e.getMessage());
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   public void testConnectionResetGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(connectionResetMessage), 2));
      clientConfig.put("generalizeSocketException", "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation succeeds.
      assertEquals("abc", client.invoke("abc"));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testConnectionClosedNotGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(connectionClosedMessage), 2));
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation fails.
      try
      {
         client.invoke("abc");
      }
      catch (MarshalException e)
      {
         Throwable t = e.getCause();
         assertEquals(connectionClosedMessage, t.getMessage());
      }
      catch (Throwable e)
      {
         fail("Expecteded message: " + connectionClosedMessage + ", got: " + e.getMessage());
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   public void testConnectionClosedGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(connectionClosedMessage), 2));
      clientConfig.put("generalizeSocketException", "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation succeeds.
      assertEquals("abc", client.invoke("abc"));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testBrokedPipedNotGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(brokenPipeMessage), 2));
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation fails.
      try
      {
         client.invoke("abc");
      }
      catch (MarshalException e)
      {
         Throwable t = e.getCause();
         assertEquals(brokenPipeMessage, t.getMessage());
      }
      catch (Throwable e)
      {
         fail("Expecteded message: " + brokenPipeMessage + ", got: " + e.getMessage());
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   public void testBrokedPipeGeneralized() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException(brokenPipeMessage), 2));
      clientConfig.put("generalizeSocketException", "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify invocation succeeds.
      assertEquals("abc", client.invoke("abc"));
      
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
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
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
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   public static class TestSocketFactory extends SocketFactory
   {
      IOException exception;
      int throwOn = -1;
      
      public TestSocketFactory()
      {
         exception = new SocketException("default");
      }
      public TestSocketFactory(IOException exception, int throwOn)
      {
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public Socket createSocket()
      {
         return new TestSocket(exception, throwOn);
      }
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         return new TestSocket(arg0, arg1, exception, throwOn);
      }
      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         return new TestSocket(arg0, arg1, exception, throwOn);
      }
      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         return new TestSocket(arg0, arg1, arg2, arg3, exception, throwOn);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         return new TestSocket(arg0, arg1, arg2, arg3, exception, throwOn);
      }
   }
   
   static class TestSocket extends Socket
   {
      IOException exception;
      int throwOn;
      
      public TestSocket(IOException exception, int throwOn)
      {
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public TestSocket(String host, int port, IOException exception, int throwOn) throws UnknownHostException, IOException
      {
         super(host, port);
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public TestSocket(InetAddress address, int port, IOException exception, int throwOn) throws IOException
      {
         super(address, port);
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public TestSocket(String host, int port, InetAddress localAddr, int localPort, IOException exception, int throwOn) throws IOException
      {
         super(host, port, localAddr, localPort);
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public TestSocket(InetAddress address, int port, InetAddress localAddr, int localPort, IOException exception, int throwOn) throws IOException
      {
         super(address, port, localAddr, localPort);
         this.exception = exception;
         this.throwOn = throwOn;
      }
      public OutputStream getOutputStream() throws IOException
      {
         return new TestOutputStream(super.getOutputStream(), exception, throwOn);
      }
      public String toString()
      {
         return "TestSocket[" + getLocalPort() + "->" + getPort() + "]";
      }
   }
 
   static class TestOutputStream extends OutputStream
   {
      OutputStream os;
      IOException exception;
      boolean closed;
      int throwOn;
      boolean doThrowException = true;
      public static int counter;
      
      public TestOutputStream(OutputStream os, IOException exception, int throwOn)
      {
         this.os = os;
         this.exception = exception;
         this.throwOn = throwOn;
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
         if (doThrowException && ++counter == throwOn)
         {
            throw exception;
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
         log.info("TestOutputStream: counter = " + counter + ", throwOn = " + throwOn);
         if (++counter == throwOn)
         {
            throw exception;
         }
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new IOException("closed");
         }
         try
         {
            log.info(this + " calling write()");
            doThrowException = false;
            os.write(b, off, len);
            os.flush();
            doThrowException = true;
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