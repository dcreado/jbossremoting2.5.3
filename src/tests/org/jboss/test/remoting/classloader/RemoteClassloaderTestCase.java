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

package org.jboss.test.remoting.classloader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.loading.ClassByteClassLoader;
import org.jboss.remoting.loading.ClassBytes;
import org.jboss.remoting.marshal.MarshallLoaderFactory;
import org.jboss.remoting.marshal.MarshallerLoaderConstants;
import org.jboss.remoting.marshal.MarshallerLoaderHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.SecurityUtility;


/**
 * Unit test for JBREM-1184.
 * 
 * Note that testClassNotFound() passes even in the presence of the NullPointerException
 * described in JBREM-1184, so it's not really a regression test.  However, I'm committing
 * the test for its documentation value.  The NPE should not be seen in the log file.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 15, 2010
 */
public class RemoteClassloaderTestCase extends TestCase
{
   private ByteArrayOutputStream baos;
   private PrintStream originalPrintStream;
   private Logger log;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      originalPrintStream = System.out;
      baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      setOut(ps);
      
      Logger.getLogger("org.jboss.remoting").setLevel(Level.DEBUG);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender);  
      log = Logger.getLogger(RemoteClassloaderTestCase.class);
   }

   
   public void tearDown()
   {
   }
   
   
   public void testClassNotFound() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      TestClassLoader tcl = new TestClassLoader();
      InvokerLocator loaderLocator = MarshallLoaderFactory.convertLocator(serverLocator);
      tcl.setClientInvoker(new Client(loaderLocator));
      
      try
      {
         tcl.findClass("a.b.c");
         fail("expected ClassNotFoundException");
      }
      catch (ClassNotFoundException e)
      {
         log.info("got expected ClassNotFoundException");
      }
      catch (Throwable t)
      {
         fail("expected ClassNotFoundException: got " + t);
      }
      
      setOut(originalPrintStream);
      String s = new String(baos.toByteArray());
      System.out.println(s);
      assertTrue(s.indexOf("java.lang.NullPointerException") == -1);
      assertTrue(s.indexOf("Can not load remote class bytes: server returned null class") >= 0);
      
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
      locatorURI += "/?loaderport=4873";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
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
      
      // Install TestMarshallerLoaderHandler.
      Field field = Connector.class.getDeclaredField("marshallerLoaderConnector");
      field.setAccessible(true);
      Connector marshallerLoaderConnector = (Connector) field.get(connector);
      MarshallerLoaderHandler loader = new TestMarshallerLoaderHandler(null);
      marshallerLoaderConnector.addInvocationHandler("loader", loader);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static private void setOut(final PrintStream ps)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setOut(ps);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               System.setOut(ps);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
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
   
   
   static class TestMarshallerLoaderHandler extends MarshallerLoaderHandler
   {
      public TestMarshallerLoaderHandler(List repositories)
      {
         super(repositories);
      }
      
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Map metadMap = invocation.getRequestPayload();
         String className = (String) metadMap.get(MarshallerLoaderConstants.CLASSNAME);
         return new ClassBytes(className, null);
      }
   }
   
   
   static class TestClassLoader extends ClassByteClassLoader
   {
      public Class findClass(String name) throws ClassNotFoundException
      {
         return super.findClass(name);
      }
   }
}