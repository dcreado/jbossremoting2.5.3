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
package org.jboss.test.remoting.ipv6;

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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * 
 * Unit test for JBREM-852.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 4, 2007
 * </p>
 */
public class IPv6TestCase extends TestCase
{
   private static Logger log = Logger.getLogger(IPv6TestCase.class);
   
   private static boolean firstTime = true;
   private static boolean ipv6IsAvailable = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.DEBUG);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);
         log.info("java.version: " + System.getProperty("java.version"));
         
         try
         {
            InetAddress addr = InetAddress.getByName("[::1]");
            new ServerSocket(3333, 200, addr);
         }
         catch (Exception e)
         {
            if ("Protocol family unavailable".equalsIgnoreCase(e.getMessage()) ||
                "Protocol family not supported".equalsIgnoreCase(e.getMessage()))
            {
               ipv6IsAvailable = false;
               log.info("ipV6 is not available");
            }
         }
      }
   }

   
   public void tearDown()
   {
   }


   public void testRawIPv6() throws Throwable
   {
      log.info("entering " + getName());
      if (!ipv6IsAvailable) return;
      
      final InetAddress addr = InetAddress.getByName("[::1]");

      new Thread()
      {
         public void run()
         {
            try
            {
               ServerSocket ss = new ServerSocket(4446, 200, addr);
               log.info("ss address: " + ss.getInetAddress());
               Socket s = ss.accept();
               InputStream is = s.getInputStream();
               log.info("read: " + is.read());
               s.close();
               ss.close();
            }
            catch (Exception e)
            {
               log.error("error", e);
            }
         }
      }.start();

      
      Thread.sleep(2000);
      
      Socket s = new Socket(addr, 4446);
      log.info("s address: " + s.getInetAddress());
      log.info("s local address: " + s.getLocalAddress());
      OutputStream os = s.getOutputStream();
      os.write(17);
      log.info("wrote 17");
      s.close();
      log.info(getName() + " PASSES");
      
   }
   
   
   public void testRemotingIPv6Loopback() throws Throwable
   {
      log.info("entering " + getName());
      if (!ipv6IsAvailable) return;
      
      doRemotingTest("[::1]");
      log.info(getName() + " PASSES");
   }
   
   
   public void testRemotingIPv6Any() throws Throwable
   { 
      log.info("entering " + getName());
      if (!ipv6IsAvailable) return;
      doRemotingTest("[::]");
      log.info(getName() + " PASSES");
   }   
   
   
   public void testRemotingIPv4Mapped() throws Throwable
   {
      if (!ipv6IsAvailable) return;
      doRemotingTest("[::ffff:127.0.0.1]");
   }
   
   
   protected void doRemotingTest(String host) throws Throwable
   {
      // Start server.
      InetAddress[] addresses = InetAddress.getAllByName("localhost");
      for (int i = 0; i < addresses.length; i++)
      {
         log.info("addresses[" + i + "]: " + addresses[i]);
      }

      int port = PortUtil.findFreePort(host); 
      String locatorURI = "socket://" + host + ":" + port + "/?timeout=10000";
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator1, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      client.disconnect();
      connector.stop();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

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
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
}