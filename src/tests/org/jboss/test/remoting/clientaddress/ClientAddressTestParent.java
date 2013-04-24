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
package org.jboss.test.remoting.clientaddress;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


public abstract class ClientAddressTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(ClientAddressTestParent.class);
   
   private static final String GET_ADDRESS = "getAddress";
   private static final String OPEN_CONNECTION = "openConnection";
   private static final String COPY = "copy:";
   private static final String SEND_CALLBACK = "sendCallback";
   private static final int ANSWER = 17;
   
   private static boolean firstTime = true;
   
   protected Connector connector;
   protected InvokerLocator serverLocator;
   protected String locatorURI;
   protected String host;
   protected int port;
   protected TestInvocationHandler invocationHandler;
   protected int callbackPort;

   
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
   
   
   /**
    * Verifies that a valid InetAddress for the client is passed to the
    * org.jboss.remoting.ServerInvocationHandler in the
    * org.jboss.remoting.InvocationRequest requestPayload.
    * @throws Throwable
    */
   public void testClientAddress() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator1, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test client address facility.
      final InetAddress localAddressFromServer = (InetAddress) client.invoke(GET_ADDRESS);
      log.info("local address according to server: " + localAddressFromServer);
      final int randomPort = PortUtil.findFreePort(localAddressFromServer.getHostAddress());
      callbackPort = randomPort;
      
      class TestThread extends Thread
      {
         public int result = -1;
         
         public void run()
         {
            try
            {
               ServerSocket ss = new ServerSocket(randomPort, 200, localAddressFromServer);
               log.info("created ServerSocket bound to: " + ss.getLocalSocketAddress());
               Socket s = ss.accept();
               log.info("accepted socket: " + s);
               InputStream is = s.getInputStream();
               result = is.read();
               log.info("read: " + result);
               s.close();
               ss.close();
            }
            catch (Exception e)
            {
               log.error(e);
            }
         }
      };
      
      TestThread t = new TestThread();
      t.start();
      Thread.sleep(5000);
      HashMap metadata = new HashMap();
      metadata.put("callbackPort", new Integer(callbackPort));
      client.invoke(OPEN_CONNECTION, metadata);
      Thread.sleep(5000);
      assertEquals(ANSWER, t.result);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that the address returned by
    * org.jboss.remoting.Client.getAddressSeenByServer() is valid.
    */
   public void testGetAddressSeenByServer() throws Throwable
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
      log.info("client is connected: " + locatorURI);
      
      // Test connections.
      assertEquals("abc", client.invoke("copy:abc"));
      log.info("connection is good");
      
      // Get address as seen by server and create callback Connector with that address.
      InetAddress callbackAddress = client.getAddressSeenByServer();
      log.info("client address seen by server: " + callbackAddress);
      String callbackHost = callbackAddress.getHostAddress();
      int callbackPort = PortUtil.findFreePort(callbackHost);
      String callbackLocatorURI = getCallbackTransport() + "://" + callbackHost + ":" + callbackPort;
      callbackLocatorURI += "/?timeout=10000";
      InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
      log.info("callback locator: " + callbackLocator);
      HashMap callbackConfig = new HashMap();
      addExtraCallbackConfig(callbackConfig);
      Connector callbackConnector = new Connector(callbackLocator, callbackConfig);
      callbackConnector.start();
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, callbackLocator);
      
      // Tell server to send a callback, and verify it was received.
      client.invoke(SEND_CALLBACK);
      assertEquals(1, callbackHandler.counter);
      
      client.removeListener(callbackHandler);
      callbackConnector.stop();
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();
   
   
   protected String getCallbackTransport()
   {
      return getTransport();
   }
   
   
   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
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
      connector.start();
   }
   
   
   protected void shutdownServer()
   {
      if (connector != null)
         connector.stop();
   }
   
   
   protected String reconstructLocator(InetAddress address)
   {
      return getTransport() + "://" + address.getHostAddress() + ":" + port;

   }
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   protected void addExtraCallbackConfig(Map config) {}
   

   class TestInvocationHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = callbackHandler;
      }
      
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object o = invocation.getParameter();
         if (! (o instanceof String))
            throw new Exception("command should be a String: " + o);
         
         String command = (String) o;
         log.info("command: " + command);
         
         if (GET_ADDRESS.equals(command))
         {
            return invocation.getRequestPayload().get(Remoting.CLIENT_ADDRESS);
         }
         else if (OPEN_CONNECTION.equals(command))
         {
            InetAddress addr = (InetAddress) invocation.getRequestPayload().get(Remoting.CLIENT_ADDRESS);
            log.info("creating socket connected to: " + addr);
            Integer callbackPortInt = (Integer) invocation.getRequestPayload().get("callbackPort");
            int callbackPort = callbackPortInt.intValue();
            Socket s = new Socket(addr, callbackPort);
            log.info("created socket connected to: " + addr);
            OutputStream os = s.getOutputStream();
            os.write(ANSWER);
            log.info("wrote answer");
            s.close();
            return null;
         }
         else if (SEND_CALLBACK.equals(command))
         {
            callbackHandler.handleCallback(new Callback("callback"));
            return null;
         }
         else if (command.startsWith(COPY))
         {
            return command.substring(5);
         }
         else
         {
            throw new Exception("unrecognized command: " + command);
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.info("received callback");
      }      
   }
}