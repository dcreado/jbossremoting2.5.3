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

package org.jboss.test.remoting.connection;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;


/**
 * Unit test for JBREM-1166.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 13, 2009
 */
public class ConnectionValidatorNPETestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionValidatorNPETestCase.class);
   private static boolean firstTime = true;
   private static int COUNT = 2000;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected SynchronizedInt errors = new SynchronizedInt(0);

   
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
   
   
   public void testNPE() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      
      // Start busy thread.
      new Thread()
      {
         public void run()
         {
            int i = 0;
            while (true)
            {
               i++;
            }
         }
      }.start();
      
      // Create ConnectionValidator multiiple times.
      HashMap metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "1");
      TestConnectionValidator[] validators = new TestConnectionValidator[COUNT];
      for (int i = 0; i < COUNT; i++)
      {
         validators[i] = new TestConnectionValidator(i, errors, client, metadata);
         validators[i].addConnectionListener(client, new TestConnectionListener());
      }
      
      Thread.sleep(5000);
      assertEquals(0, errors.get());
      
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
   

   static class TestConnectionListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {
      }
   }
   
   static class TestConnectionValidator extends ConnectionValidator
   {
      public volatile boolean timerWasNull;
      int id;
      Rendezvous rendezvous;
      boolean[] flags;
      SynchronizedInt errors;
      Field field;
      
      public TestConnectionValidator(int id, SynchronizedInt errors, Client client, Map metadata) throws SecurityException, NoSuchFieldException
      {
         super(client, metadata);
         this.id = id;
         this.errors = errors;
         field = ConnectionValidator.class.getDeclaredField("timer");
         field.setAccessible(true);
      }
      
      public void run()
      {
         try
         {
//            log.info(id + ": starting");
            timerWasNull = (field.get(this) == null);
            if (timerWasNull)
            {
               log.info("timer was null: " + id);
               errors.increment();
            }
         }
         catch (Exception e)
         {
            log.info("exception", e);
         }
      }
   }
}