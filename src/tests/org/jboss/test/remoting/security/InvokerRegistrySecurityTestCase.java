/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ClientFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


public class InvokerRegistrySecurityTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(InvokerRegistrySecurityTestCase.class);
   private static boolean firstTime = true;

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


   public void testRegisterUnregisterInvokerFactories() throws Throwable
   {
      log.info("entering " + getName());

      // Call from code with proper privileges.
      InvokerRegistry.registerInvokerFactories("test", TestClientFactory.class, TestServerFactory.class);
      InvokerRegistry.unregisterInvokerFactories("test");

      // Call from code without proper privileges.
      if (System.getSecurityManager() == null)
      {
         InvokerRegistryCaller.registerInvokerFactories("test", TestClientFactory.class, TestServerFactory.class);
         InvokerRegistryCaller.unregisterInvokerFactories("test"); 
      }
      else
      {
         try
         {
            InvokerRegistryCaller.registerInvokerFactories("test", TestClientFactory.class, TestServerFactory.class); 
            fail("expected SecurityException");
         }
         catch (SecurityException e)
         {
            log.info("got expected SecurityException");
         }
         try
         {
            InvokerRegistryCaller.unregisterInvokerFactories("test");
            fail("expected SecurityException");
         }
         catch (SecurityException e)
         {
            log.info("got expected SecurityException");
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testUnregisterLocator() throws Throwable
   {
      log.info("entering " + getName());
      Connector connector = null;

      try
      {
         // Create server, which stores InvokerLocator in InvokerRegistry.
         InvokerLocator locator = new InvokerLocator("socket://localhost");
         connector = new Connector(locator);
         connector.start();
         InvokerLocator updatedLocator = connector.getLocator();

         // Call from code with proper privileges.
         InvokerRegistry.unregisterLocator(updatedLocator);

         // Call from code without proper privileges.
         if (System.getSecurityManager() == null)
         {
            InvokerRegistryCaller.unregisterLocator(updatedLocator);  
         }
         else
         {
            try
            {
               InvokerRegistryCaller.unregisterLocator(updatedLocator); 
               fail("expected SecurityException");
            }
            catch (SecurityException e)
            {
               log.info("got expected SecurityException");
            }
         }
      }
      finally
      {
         if (connector != null)
         {
            connector.stop();
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testDestroyClientInvoker() throws Throwable
   {
      log.info("entering " + getName());
      Client client = null;

      try
      {
         // Create client.
         InvokerLocator locator = new InvokerLocator("socket://localhost");
         client = new Client(locator);
         client.connect();

         // Call from code with proper privileges.
         InvokerRegistry.destroyClientInvoker(locator, new HashMap());

         // Call from code without proper privileges.
         if (System.getSecurityManager() == null)
         {
            InvokerRegistryCaller.destroyClientInvoker(locator, new HashMap()); 
         }
         else
         {
            try
            {
               InvokerRegistryCaller.destroyClientInvoker(locator, new HashMap()); 
               fail("expected SecurityException");
            }
            catch (SecurityException e)
            {
               log.info("got expected SecurityException");
            }
         }
      }
      finally
      {
         if (client != null)
         {
            client.disconnect();
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testCreateClientInvoker() throws Throwable
   {
      log.info("entering " + getName());
      InvokerLocator locator = new InvokerLocator("socket://localhost");

      // Call from code with proper privileges.
      ClientInvoker invoker = InvokerRegistry.createClientInvoker(locator, new HashMap());;

      try
      {
         // Call from code without proper privileges.
         if (System.getSecurityManager() == null)
         {
            InvokerRegistryCaller.createClientInvoker(locator, new HashMap());
         }
         else
         {
            try
            {
               InvokerRegistryCaller.createClientInvoker(locator, new HashMap());
               fail("expected SecurityException");
            }
            catch (SecurityException e)
            {
               log.info("got expected SecurityException");
            }
         }
      }
      finally
      {
         if (invoker != null)
         {
            InvokerRegistry.destroyClientInvoker(locator, new HashMap());
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testCreateServerInvoker() throws Throwable
   {
      log.info("entering " + getName());
      InvokerLocator locator = new InvokerLocator("socket://localhost");

      // Call from code with proper privileges.
      ServerInvoker invoker = InvokerRegistry.createServerInvoker(locator, new HashMap());;

      try
      {
         // Call from code without proper privileges.
         if (System.getSecurityManager() == null)
         {
            InvokerRegistryCaller.createServerInvoker(locator, new HashMap());
         }
         else
         {
            try
            {
               InvokerRegistryCaller.createServerInvoker(locator, new HashMap());
               fail("expected SecurityException");
            }
            catch (SecurityException e)
            {
               log.info("got expected SecurityException");
            }
         }
      }
      finally
      {
         if (invoker != null)
         {
            InvokerRegistry.destroyServerInvoker(invoker);
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testDestroyServerInvoker() throws Throwable
   {
      log.info("entering " + getName());
      InvokerLocator locator = new InvokerLocator("socket://localhost");
      ServerInvoker invoker = InvokerRegistry.createServerInvoker(locator, new HashMap());;

      // Call from code with proper privileges.
      InvokerRegistry.destroyServerInvoker(invoker);

      // Call from code without proper privileges.
      if (System.getSecurityManager() == null)
      {
         InvokerRegistryCaller.destroyServerInvoker(invoker);
      }
      else
      {
         try
         {
            InvokerRegistryCaller.destroyServerInvoker(invoker);
            fail("expected SecurityException");
         }
         catch (SecurityException e)
         {
            log.info("got expected SecurityException");
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testUpdateServerInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      InvokerLocator oldLocator = new InvokerLocator("socket://localhost");
      InvokerLocator newLocator = new InvokerLocator("socket://localhost");
      ServerInvoker invoker = InvokerRegistry.createServerInvoker(oldLocator, new HashMap());;

      try 
      {
         // Call from code with proper privileges.
         InvokerRegistry.updateServerInvokerLocator(oldLocator, newLocator);

         // Call from code without proper privileges.
         if (System.getSecurityManager() == null)
         {
            InvokerRegistryCaller.updateServerInvokerLocator(oldLocator, newLocator);
         }
         else
         {
            try
            {
               InvokerRegistryCaller.updateServerInvokerLocator(oldLocator, newLocator);
               fail("expected SecurityException");
            }
            catch (SecurityException e)
            {
               log.info("got expected SecurityException");
            }
         }
      }
      finally
      {
         if (invoker != null)
         {
            InvokerRegistry.destroyServerInvoker(invoker);
         }
      }

      log.info(getName() + " PASSES");
   }

   public void testIsSSLSupported() throws Throwable
   {
      log.info("entering " + getName());

      // Call from code with proper privileges.
      InvokerRegistry.isSSLSupported("bisocket");

      // Call from code without proper privileges.
      if (System.getSecurityManager() == null)
      {
         InvokerRegistryCaller.isSSLSupported("http");
      }
      else
      {
         try
         {
            InvokerRegistryCaller.isSSLSupported("http");
            fail("expected SecurityException");
         }
         catch (SecurityException e)
         {
            log.info("got expected SecurityException");
         }
      }

      log.info(getName() + " PASSES");
   }
   
   static class TestServerFactory implements ServerFactory
   {
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
      {
         return new SocketServerInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }

   static class TestClientFactory implements ClientFactory
   {
      public ClientInvoker createClientInvoker(InvokerLocator locator, Map config) throws IOException
      {
         return new SocketClientInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
}