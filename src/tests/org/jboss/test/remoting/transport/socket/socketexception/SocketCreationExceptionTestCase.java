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
package org.jboss.test.remoting.transport.socket.socketexception;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/**
 * Unit tests for JBREM-1152.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Sep 9, 2009
 * </p>
 */
public class SocketCreationExceptionTestCase extends TestCase
{
   protected static String SEND_CALLBACK = "sendCallback";
   private static Logger log = Logger.getLogger(SocketCreationExceptionTestCase.class);
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
   
   
   public void testInvocationWithSocketException() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new SocketException(getName()), 2));
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testInvocationWithIOException() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, new TestSocketFactory(new IOException("Connection reset"), 2));
      clientConfig.put("generalizeSocketException", "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
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
   

   protected void setupServer(ServerSocketFactory ssf) throws Exception
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
      config.put("timeout", "5000");
      if (ssf != null)
      {
         config.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
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
      InvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = callbackHandler;
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object parameter = invocation.getParameter();
         if (SEND_CALLBACK.equals(parameter))
         {
            try
            {
               callbackHandler.handleCallback(new Callback("callback"));
            }
            catch (HandleCallbackException e)
            {
               log.error("unable to send callback", e);
            }
         }
         return parameter;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   public static class TestSocketFactory extends SocketFactory
   {
      int counter;
      int limit; 
      IOException exception;
      
      public TestSocketFactory()
      {
      }
      public TestSocketFactory(IOException exception, int limit)
      {
         this.exception = exception;
         this.limit = limit;
      }
      public Socket createSocket() throws IOException
      {
         counter++;
         log.info("counter: " + counter);
         if (counter <= limit)
         {
            log.info("throwing exception");
            throw exception;
         }
         log.info("returning socket");
         return new Socket();
      }
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         counter++;
         log.info("counter: " + counter);
         if (counter <= limit)
         {
            throw exception;
         }
         log.info("returning socket");
         return new Socket(arg0, arg1);
      }
      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         counter++;
         log.info("counter: " + counter);
         if (counter <= limit)
         {
            throw exception;
         }
         log.info("returning socket");
         return new Socket(arg0, arg1);
      }
      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         counter++;
         log.info("counter: " + counter);
         if (counter <= limit)
         {
            throw exception;
         }
         log.info("returning socket");
         return new Socket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         counter++;
         log.info("counter: " + counter);
         if (counter <= limit)
         {
            throw exception;
         }
         log.info("returning socket");
         return new Socket(arg0, arg1, arg2, arg3);
      }
   }
}