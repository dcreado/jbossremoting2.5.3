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

package org.jboss.test.remoting.lease;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.SocketFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1154.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Sep 11, 2009
 * </p>
 */
public class LeaseCreationFailureTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(LeaseCreationFailureTestCase.class);
   
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
   }

   
   public void tearDown()
   {
   }
   
   
   public void testLeaseCreationFailure() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.SOCKET_FACTORY_NAME, TestSocketFactory.class.getName());
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      
      // Verify that Client.connect() throws Exception if LeasePinger.addClient() fails.
      try
      {
         client.connect();
         fail("expected exception");
      }
      catch (CannotConnectException e)
      {
         log.info("got exception", e);
         assertEquals("got wrong exception", "Error setting up client lease upon performing connect.", e.getMessage());
      }
      catch (Throwable t)
      {
         log.info("got wrong exception", t);
         fail("got wrong exception");
      }
      assertFalse(client.isConnected());
      
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
      config.put("clientLeasePeriod", "1000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.addConnectionListener(new TestConnectionListener());
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
   
   
   static class TestConnectionListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {
      }
   }


   public static class TestSocketFactory extends SocketFactory
   {
      int initialSuccesses = 2;

      public TestSocketFactory()
      {
      }
      public TestSocketFactory(int initialSuccesses)
      {
         this.initialSuccesses = initialSuccesses;
      }
      public Socket createSocket()
      {
         Socket s = new TestSocket(initialSuccesses);
         log.info("returning " + s);
         return s;
      }
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         Socket s = new TestSocket(arg0, arg1, initialSuccesses);
         log.info("returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         Socket s = new TestSocket(arg0, arg1, initialSuccesses);
         log.info("returning " + s);
         return s;
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         Socket s = new TestSocket(arg0, arg1, arg2, arg3, initialSuccesses);
         log.info("returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         Socket s = new TestSocket(arg0, arg1, arg2, arg3, initialSuccesses);
         log.info("returning " + s);
         return s;
      }
   }
   
   
   static class TestSocket extends Socket
   {
      int initialSuccesses;
      
      public TestSocket(int initialWrites)
      {
         this.initialSuccesses = initialWrites;
      }
      public TestSocket(String host, int port, int initialWrites) throws UnknownHostException, IOException
      {
         super(host, port);
         this.initialSuccesses = initialWrites;
      }
      public TestSocket(InetAddress address, int port, int initialWrites) throws IOException
      {
         super(address, port);
         this.initialSuccesses = initialWrites;
      }
      public TestSocket(String host, int port, InetAddress localAddr, int localPort, int initialWrites) throws IOException
      {
         super(host, port, localAddr, localPort);
         this.initialSuccesses = initialWrites;
      }
      public TestSocket(InetAddress address, int port, InetAddress localAddr, int localPort, int initialWrites) throws IOException
      {
         super(address, port, localAddr, localPort);
         this.initialSuccesses = initialWrites;
      }
      public OutputStream getOutputStream() throws IOException
      {
         return new TestOutputStream(super.getOutputStream(), initialSuccesses);
      }
      public String toString()
      {
         return "TestSocket[" + getLocalPort() + "->" + getPort() + "]";
      }
   }
   
   
   static class TestOutputStream extends OutputStream
   {
      OutputStream os;
      boolean closed;
      int initialWrites;
      boolean doThrow = true;
      public static int counter;
      
      public TestOutputStream(OutputStream os, int initialWrites)
      {
         this.os = os;
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
         if (doThrow && ++counter > initialWrites)
         {
            log.info("throwing exception");
            throw new IOException("");
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
         log.info("TestOutputStream: counter = " + ++counter + ", initialWrites = " + initialWrites);
         if (counter > initialWrites)
         {
            log.info("throwing exception");
            throw new IOException("");
         }
         if (closed)
         {
            log.info("TestOutputStream closed, cannot write");
            throw new IOException("closed");
         }
         try
         {
            log.info(this + " calling write()");
            doThrow = false;
            os.write(b, off, len);
            os.flush();
            doThrow = true;
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