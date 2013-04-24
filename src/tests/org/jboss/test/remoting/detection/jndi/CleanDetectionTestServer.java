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
package org.jboss.test.remoting.detection.jndi;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.AbstractDetector;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jnp.server.Main;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class CleanDetectionTestServer extends ServerTestCase
{
   public static int syncPort = 6502;
   
   protected static Logger log = Logger.getLogger(CleanDetectionTestServer.class);
   protected static String transport;
   protected static int port = 5402;
   protected static Thread serverThread;
   
   protected int detectorPort = 1099;
   protected String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   protected String urlPackage = "org.jboss.naming:org.jnp.interfaces";
   protected Main jserver;
   protected JNDIDetector detector;
   protected Connector connector;

   public void setUp() throws Exception
   {
      final String host = InetAddress.getLocalHost().getHostName();
      
      serverThread = new Thread()
      {
         public void run()
         {
            try
            {
               setupJNDI();
               setupServer(host);
               setupDetector();
               ServerSocket ss = new ServerSocket(syncPort, 0, InetAddress.getLocalHost());
               log.info("bound to: " + InetAddress.getLocalHost() + ":" + syncPort);
               Socket s = ss.accept();
               InputStream is = s.getInputStream();
               OutputStream os = s.getOutputStream();
               
               // Indicate server is started.
               os.write(3);
               log.info("indicated server started");
               
               is.read();
               log.info("got request to shut down server");
               shutdownServer();
               disableDetector();
               log.info("shut down server");
               
               is.read();
               log.info("got request to restart server");
               setupServer(host);
               setupDetector();
               os.write(7);
               log.info("restarted server");
               
               is.read();
               log.info("got request to shut down server");
               shutdownServer();
               shutdownDetector();
               shutdownJNDI();
               log.info("shut down server");
            }
            catch (Exception e)
            {
               log.error(e);
               e.printStackTrace();
            }
         }
      };
      
      serverThread.start();
   }
   
   protected void setupJNDI() throws Exception
   {
      Object namingBean = null;
      Class namingBeanImplClass = null;
      try
      {
         namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
         namingBean = namingBeanImplClass.newInstance();
         Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
         setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
         namingBeanImplStart(namingBean, startMethod);
      }
      catch (Exception e)
      {
         SimpleJNDIServer.println("Cannot find NamingBeanImpl: must be running jdk 1.4");
      }

      String host = InetAddress.getLocalHost().getHostAddress();
      
      jserver = new Main();
      jserver.setPort(detectorPort);
      jserver.setBindAddress(host);
      jserver.setRmiPort(31000);
      if (namingBean != null)
      {
         Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
         Method setNamingInfoMethod = jserver.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
         setNamingInfoMethod.invoke(jserver, new Object[] {namingBean});
      }
      jserver.start();
      System.out.println("Started JNDI server on " + host + ":" + port);
   }
   
   protected void shutdownJNDI()
   {
      jserver.stop();
   }

   protected void setupDetector() throws Exception
   {
      // we need an MBeanServer to store our network registry and multicast detector services
      MBeanServer server = MBeanServerFactory.createMBeanServer();

      String detectorHost = InetAddress.getLocalHost().getHostName();

      detector = new JNDIDetector(getConfiguration());
      // set config info for detector and start it.
      detector.setPort(detectorPort);
      detector.setHost(detectorHost);
      detector.setContextFactory(contextFactory);
      detector.setURLPackage(urlPackage);

      server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));
      detector.setCleanDetectionNumber(detector.getCleanDetectionNumber() * 2);
      detector.start();
      log.info("JNDIDetector has been created and is listening for new NetworkRegistries to come online");
   }
   
   
   protected void shutdownDetector() throws Exception
   {
      detector.stop();
   }

   
   protected void disableDetector() throws Exception
   {
      Field field = AbstractDetector.class.getDeclaredField("heartbeatTimer");
      field.setAccessible(true);
      Timer heartbeatTimer = (Timer) field.get(detector);
      heartbeatTimer.cancel();
   }
   
   
   /**
    * Sets up our JBoss/Remoting server by creating our Connector on the given locator URI
    * and installing our invocation handler that will handle incoming messages.
    *
    * @param locatorURI defines our server endpoing
    * @throws Exception
    */
   protected void setupServer(String host) throws Exception
   {
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector(locator, getConfiguration());
      connector.create();
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer()
   {
      String locatorURI = connector.getLocator().getLocatorURI();
      connector.stop();
      log.info("Stopped remoting server with locator uri of: " + locatorURI);
   }

   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   /**
    * @return configuration map for Connector and JNDIDetector
    */
   protected Map getConfiguration()
   {
      return new HashMap();
   }
   
   
   public static void main(String[] args)
   {
      try
      {
         CleanDetectionTestServer server = new CleanDetectionTestServer();
         server.setUp();
         serverThread.join();
      }
      catch (Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
   }
   

   /**
    * Simple invocation handler implementation.  This is the handler that processes incoming messages from clients.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      /**
       * This is the method that is called when a new message comes in from a client.
       *
       * @param invocation the incoming client invocation, encapsulates the message object
       * @return the response object we send back to the client.
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Print out the invocation request
         String msg = invocation.getParameter().toString();

         log.info("RECEIVED A CLIENT MESSAGE: " + msg);

         String response = "Server received your message that said [" + msg + "]";

         if(msg.indexOf("Welcome") > -1)
         {
            response = "Received your welcome message.  Thank you!";
         }

         log.info("Returning the following message back to the client: " + response);

         return response;
      }

      /**
       * Adds a callback handler that will listen for callbacks from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as we do not handle callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as we do not handle callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as we do not need a reference to the MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as we do not need a reference back to the server invoker
      }
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
   
   static private void namingBeanImplStart(final Object namingBean, final Method startMethod)
   throws IllegalAccessException, InvocationTargetException
   {
      if (SecurityUtility.skipAccessControl())
      {
         startMethod.invoke(namingBean, new Object[] {});
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction() 
         {
            public Object run() throws IllegalAccessException, InvocationTargetException
            {
               startMethod.invoke(namingBean, new Object[] {});
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof IllegalAccessException)
            throw (IllegalAccessException) cause;
         else
            throw (InvocationTargetException) cause;
      }
   }
}
