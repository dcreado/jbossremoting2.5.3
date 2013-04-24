/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.transport.servlet.callback;

import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * Unit test for pull callbacks over servlet transport: JBREM-1079.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 16, 2009
 * </p>
 */
public class CallbackTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackTestClient.class);
   
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
   
   
   public void testMethod() throws Throwable
   {
      log.info("entering " + getName());

      // Create client.
      locatorURI = "servlet://localhost:8080/servlet-invoker/ServerInvokerServlet";
      locatorURI += "/?createUniqueObjectName=true&useAllParams=true&blockingMode=blocking";
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      log.info("result: " + client.invoke("abc"));
      assertEquals(null, client.invoke("abc"));
      log.info("connection is good");
      
      // Install client side callback handlers.
      TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
      TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      client.addListener(callbackHandler1, metadata);
      client.addListener(callbackHandler2, metadata);
      
      // Request callbacks.
      int COUNT = 100;
      for (int i = 0; i < COUNT; i++)
      {
         client.invoke("callback");
      }
      
      log.info("sleeping for 2000 ms");
      Thread.sleep(2000);
      log.info("waking up");
      
      // Verify all callbacks arrived.
      assertEquals(COUNT, callbackHandler1.counter);
      assertEquals(COUNT, callbackHandler1.counter);
      
      client.removeListener(callbackHandler1);
      client.removeListener(callbackHandler2);
      client.disconnect();
      log.info(getName() + " PASSES");
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
      }  
   }
}