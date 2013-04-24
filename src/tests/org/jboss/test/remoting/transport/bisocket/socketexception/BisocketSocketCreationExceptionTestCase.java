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
package org.jboss.test.remoting.transport.bisocket.socketexception;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ServerSocketFactory;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.test.remoting.transport.socket.socketexception.SocketCreationExceptionTestCase;

/**
 * Unit tests for JBREM-1152.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Sep 9, 2009
 * </p>
 */
public class BisocketSocketCreationExceptionTestCase extends SocketCreationExceptionTestCase
{
   private static Logger log = Logger.getLogger(BisocketSocketCreationExceptionTestCase.class);
   
   
   public void testCallbackException() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(new TestServerSocketFactory(2, new SocketException(getName())));
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      
      // Get client side ServerThread pool.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      SocketServerInvoker serverInvoker = (SocketServerInvoker) callbackConnector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      
      // Verify MicroSocketClientInvoker retries invocation after failure to get a connection.
      client.invoke(SEND_CALLBACK);
      assertEquals(1, callbackHandler.received);
      Set set = clientpool.getContents();   
      Object[] serverThreads = set.toArray();
      for (int i = 0; i < serverThreads.length; i++)
      {
         ServerThread st = (ServerThread) serverThreads[i];
         st.shutdown();
      }
      client.invoke(SEND_CALLBACK);
      assertEquals(2, callbackHandler.received);
      set = clientpool.getContents();
      serverThreads = set.toArray();
      for (int i = 0; i < serverThreads.length; i++)
      {
         ServerThread st = (ServerThread) serverThreads[i];
         st.shutdown();
      }
      client.invoke(SEND_CALLBACK);
      assertEquals(3, callbackHandler.received);
      
      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int received;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
         received++;
      }  
   }
   
   static public class TestServerSocketFactory extends ServerSocketFactory
   {
      int initialSuccesses;
      IOException exception;
      
      public TestServerSocketFactory()
      {
         this.initialSuccesses = -1;
         this.exception = new IOException();
      }      
      public TestServerSocketFactory(int initialSuccesses, IOException exception)
      {
         this.initialSuccesses = initialSuccesses;
         this.exception = exception;
      }
      public ServerSocket createServerSocket() throws IOException
      {
         ServerSocket ss = new TestServerSocket(initialSuccesses, exception);
         log.info(this + " returning: " + ss);
         return ss;
      }
      public ServerSocket createServerSocket(int port) throws IOException
      {
         ServerSocket ss = new TestServerSocket(port, initialSuccesses, exception);
         log.info(this + " returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog) throws IOException
      {
         ServerSocket ss = new TestServerSocket(port, backlog, initialSuccesses, exception);
         log.info(this + " returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException
      {
         ServerSocket ss = new TestServerSocket(port, backlog, ifAddress, initialSuccesses, exception);
         log.info(this + " returning: " + ss);
         return ss;
      }
   }
   
   
   static class TestServerSocket extends ServerSocket
   {
      int initialSuccesses;
      IOException exception;
      int counter;

      public TestServerSocket(int initialSuccesses, IOException exception) throws IOException
      {
         super();
         this.initialSuccesses = initialSuccesses;
         this.exception = exception;
      }
      public TestServerSocket(int port, int initialSuccesses, IOException exception) throws IOException
      {
         super(port);
         this.initialSuccesses = initialSuccesses;
         this.exception = exception;
      }
      public TestServerSocket(int port, int backlog, int initialSuccesses, IOException exception) throws IOException
      {
         super(port, backlog);
         this.initialSuccesses = initialSuccesses;
         this.exception = exception;
      }
      public TestServerSocket(int port, int backlog, InetAddress bindAddr, int initialSuccesses, IOException exception) throws IOException
      {
         super(port, backlog, bindAddr);
         this.initialSuccesses = initialSuccesses;
         this.exception = exception;
      }
      public Socket accept() throws IOException
      {
         ++counter;
         Socket s = super.accept();
         log.info(this + " counter: " + counter);
         if (counter > initialSuccesses && counter <= initialSuccesses + 2)
         {
            throw exception;
         }
         log.info(this + " returning: " + s);
         return s;
      }
      public String toString()
      {
         return "TestServerSocket[" + getLocalPort() + "]";
      }
   }
   
}