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
package org.jboss.test.remoting.classloader.race;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * 
 * Unit tests for JBREM-900.
 * 
 * Note: The class org.jboss.test.remoting.classloader.race.TestObject mentioned
 * in the tests below is found in 
 * 
 *  <remoting home>/src/etc/org/jboss/test/remoting/classloader/race.
 *    
 * It is loaded from
 * 
 *  <remoting home>/output/tests/classes/org/jboss/test/remoting/classloader/race/test.jar.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 19, 2008
 * </p>
 */
public abstract class ClassloaderRaceTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(ClassloaderRaceTestParent.class);
   
   private static boolean firstTime = true;
   protected static String metadata;
   
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
         metadata = System.getProperty("remoting.metadata", "serializationtype=java");
      }
   }

   
   public void tearDown()
   {
   }
   
   public void testDirectClassloading() throws Throwable
   {
      log.info("entering " + getName());

      URL url = ClassloaderRaceTestParent.class.getResource("test.jar");
      ClassLoader cl1 = new TestClassLoader(new URL[]{url});
      Class c1 = cl1.loadClass("org.jboss.test.remoting.classloader.race.TestObject");
      Object testObject1 = c1.newInstance();
      log.info("classloader1: " + testObject1.getClass().getClassLoader());

      ClassLoader cl2 = new TestClassLoader(new URL[]{url});
      Class c2 = cl2.loadClass("org.jboss.test.remoting.classloader.race.TestObject");
      Object testObject2 = c2.newInstance();
      log.info("classloader2: " + testObject2.getClass().getClassLoader());

      assertFalse(testObject1.getClass().isAssignableFrom(testObject2.getClass()));            
      log.info(getName() + " PASSES");
   }
   
   
   public void testSequential() throws Throwable
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
      log.info("client is connected to: " + locatorURI);
      
      SimpleInvocationThread t1 = new SimpleInvocationThread(client, "InvocationThread:1");
      t1.start();
      t1.join();
      SimpleInvocationThread t2 = new SimpleInvocationThread(client, "InvocationThread:2");
      t2.start();
      t2.join();
      
      assertEquals(t1.getContextClassLoader(), t1.getResponseClassLoader());
      assertEquals(t2.getContextClassLoader(), t2.getResponseClassLoader());
      assertNotSame(t1.getResponseClassLoader(), t2.getResponseClassLoader());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSimultaneous() throws Throwable
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
      log.info("client is connected to: " + locatorURI);
      
      int COUNT = 100;
      log.info("COUNT: " + COUNT);
      SynchronizedInvocationThread[] threads = new SynchronizedInvocationThread[COUNT];
      Object lock = new Object();
      URL url = ClassloaderRaceTestParent.class.getResource("test.jar");
      ClassLoader cl1 = new TestClassLoader(new URL[]{url});
      ClassLoader cl2 = new TestClassLoader(new URL[]{url});
      log.info("classloader1: " + cl1);
      log.info("classloader2: " + cl2);
      
      // Create threads that use cl1.
      for (int i = 0; i < COUNT / 2; i++)
      {
         threads[i] = new SynchronizedInvocationThread(client, cl1, i, lock);
         threads[i].start();
      }
      
      // Create threads that use cl2.
      for (int i = COUNT/2; i < COUNT; i++)
      {
         threads[i] = new SynchronizedInvocationThread(client, cl2, i, lock);
         threads[i].start();
      }
      
      // Start threads.
      Thread.sleep(2000);
      synchronized (lock)
      {
         lock.notifyAll();
      }
      
      // Wait for all threads to complete.
      for (int i = 0; i < COUNT; i++)
      {
         threads[i].join();
      }
      
      // Checks threads that use cl1.
      for (int i = 0; i < COUNT / 2; i++)
      {
         assertEquals("error in thread " + i, cl1, threads[i].responseClassLoader);     
      }
      
      // Check threads that use cl2.
      for (int i = COUNT / 2; i < COUNT; i++)
      {
         assertEquals("error in thread " + i, cl2, threads[i].responseClassLoader);     
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }

   
   protected abstract String getTransport();
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?" + metadata;
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
         URL url = ClassloaderRaceTestParent.class.getResource("test.jar");
         ClassLoader cl1 = new TestClassLoader(new URL[]{url});
         Class c = cl1.loadClass("org.jboss.test.remoting.classloader.race.TestObject");
         Object o = c.newInstance(); 
         return o;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestClassLoader extends URLClassLoader
   {
      public TestClassLoader(URL[] urls)
      {
         super(urls, null);
      }

      public Class findClass(String fqn) throws ClassNotFoundException
      {
         log.debug(this + " loading class: " + fqn);
         Class c = super.findClass(fqn);
         log.debug(this + " loaded class: " + fqn);
         return c;
      }
   }
   
   
   static class SimpleInvocationThread extends Thread
   {
      Client client;
      String name;
      ClassLoader contextClassLoader;
      ClassLoader responseClassLoader;
      
      public SimpleInvocationThread(Client client, String name)
      {
         this.client = client;
         this.name = name;
      }

      public void run()
      {
         try
         {
            URL url = getClass().getResource("test.jar");
            contextClassLoader = new TestClassLoader(new URL[]{url});
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            log.info(this + " context classloader: " + contextClassLoader);
            Object response = client.invoke(name);
            responseClassLoader = response.getClass().getClassLoader();
            log.info(this + " response classloader: " + responseClassLoader);
         }
         catch (Throwable t)
         {
            log.error("unable to complete invocation", t);
         }
      }
      
      public ClassLoader getContextClassLoader()
      {
         return contextClassLoader;
      }
      
      public ClassLoader getResponseClassLoader()
      {
         return responseClassLoader;
      }
      
      public String toString()
      {
         return name;
      }
   }
   
   
   static class SynchronizedInvocationThread extends Thread
   {
      Client client;
      String name;
      ClassLoader contextClassLoader;
      ClassLoader responseClassLoader;
      Object lock;
      
      public SynchronizedInvocationThread(Client client, ClassLoader classLoader,
                                          int id, Object lock)
      {
         this.client = client;
         this.contextClassLoader = classLoader;
         this.name = "SynchronizedInvocationThread:" + id;
         this.lock = lock;
      }

      public void run()
      {
         try
         {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            
            synchronized (lock)
            {
               log.debug(this + " waiting");
               lock.wait();
            }
            
            log.debug(this + " making invocation");
            Object response = client.invoke(name);
            responseClassLoader = response.getClass().getClassLoader();
            log.debug(this + " done");
         }
         catch (Throwable t)
         {
            log.error(this + " unable to complete invocation", t);
         }
      }
      
      public String toString()
      {
         return name;
      }
   }
   
   
   static class Counter
   {
      public int count;
   }
}