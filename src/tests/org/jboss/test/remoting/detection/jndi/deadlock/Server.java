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
package org.jboss.test.remoting.detection.jndi.deadlock;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.sslmultiplex.SSLMultiplexServerInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jnp.server.Main;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author Michael Voss
 */
public class Server implements Runnable
{
   private static Connector connector = null;
   private static Logger logger;
   private MBeanServer server;
   private JNDIDetector detector;

   private String jndiAddress = null;
   private int jndiPort = 2410;
   private String port = "1001";


   private void init()
   {
      String localHost = "";
      try
      {
         jndiAddress = localHost = InetAddress.getLocalHost().getHostAddress();
      }
      catch (UnknownHostException uhe)
      {
         uhe.printStackTrace();
         System.exit(1);
      }

      try
      {
//         String serverKeyStorePath = this.getClass().getResource("certificate/serverKeyStore").getFile();
//         String serverTrustStorePath = this.getClass().getResource("certificate/serverTrustStore").getFile();
         String serverKeyStorePath = this.getClass().getResource("../../../transport/socket/ssl/.keystore").getFile();
         String serverTrustStorePath = this.getClass().getResource("../../../transport/socket/ssl/.trustStore").getFile();

         Map configuration = new HashMap();
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, serverKeyStorePath);
//         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, "certificate/serverKeyStore");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "testpw");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
         configuration.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");

         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, serverTrustStorePath);
//         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, "certificate/serverTrustStore");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "testpw");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         configuration.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");

         configuration.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");

/*
         configuration.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_TYPE, "JKS");
         configuration.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_FILE_PATH, serverKeyStorePath);
//         configuration.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_FILE_PATH, "certificate/serverKeyStore");
         configuration.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_PASSWORD, "testpw");
         configuration.put(RemotingSSLSocketFactory.REMOTING_ALGORITHM, "SunX509");

         configuration.put(RemotingSSLSocketFactory.REMOTING_TRUST_STORE_TYPE, "JKS");
         configuration.put(RemotingSSLSocketFactory.REMOTING_TRUST_STORE_FILE_PATH, serverTrustStorePath);
//         configuration.put(RemotingSSLSocketFactory.REMOTING_TRUST_STORE_FILE_PATH, "certificate/serverTrustStore");
         configuration.put(RemotingSSLSocketFactory.REMOTING_TRUST_STORE_PASSWORD, "testpw");
         configuration.put(RemotingSSLSocketFactory.REMOTING_TRUST_ALGORITHM, "SunX509");
        configuration.put(RemotingSSLSocketFactory.REMOTING_USE_CLIENT_MODE, "false");
*/
          connector = new Connector(configuration);

         InvokerLocator locator = new InvokerLocator("sslmultiplex://" + localHost + ":" + port);
         connector.setInvokerLocator(locator.getLocatorURI());
         connector.create();

         try
         {

//            ServerSocketFactory svrSocketFactory = SSL.createServerSocketFactory("testpw", "testpw", "certificate/serverKeyStore", "certificate/serverTrustStore");
            ServerSocketFactory svrSocketFactory = SSL.createServerSocketFactory("unit-tests-server", "unit-tests-client", serverKeyStorePath, serverTrustStorePath);

            SSLMultiplexServerInvoker socketSvrInvoker = (SSLMultiplexServerInvoker) connector.getServerInvoker();
            socketSvrInvoker.setServerSocketFactory(svrSocketFactory);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }

         /*connector = new Connector();
            InvokerLocator locator = new InvokerLocator("multiplex://"+localHost+":"+port);
            connector.setInvokerLocator(locator.getLocatorURI());
            connector.create();	*/

         try
         {
            connector.addInvocationHandler("sample", new SampleInvocationHandler());
         }
         catch (Exception e)
         {
            e.printStackTrace();
            System.exit(1);
         }
         connector.start();

         server.registerMBean(connector, new ObjectName("jboss.remoting:type=Connector"));


      }
      catch (Exception e)
      {
         e.printStackTrace();
      }


   }


   private void registerJNDI()
   {

      detector = new JNDIDetector();

      try
      {
         server.registerMBean(detector, new ObjectName("remoting:type=JNDIDetector"));
      }
      catch (Exception ignored)
      {
      }

      detector.setPort(jndiPort);
      detector.setHost(jndiAddress);
      try
      {
         detector.start();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }


   private void setUp() throws Exception
   {

      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      try
      {
         Thread t = new Thread(this);
         Runtime.getRuntime().addShutdownHook(t);
      }
      catch (Exception ignored)
      {
      }

      logger = Logger.getLogger(getClass());
      logger.setLevel((Level) Level.DEBUG);

      server = MBeanServerFactory.createMBeanServer();
      startJNDIServer();
      init();
      registerJNDI();


   }

   private void startJNDIServer() throws Exception
   {
      Object namingBean = null;
      Class namingBeanImplClass = null;
      try
      {
         namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
         namingBean = namingBeanImplClass.newInstance();
         Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
         setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
         startMethod.invoke(namingBean, new Object[] {});
      }
      catch (Exception e)
      {
         SimpleJNDIServer.println("Cannot find NamingBeanImpl: must be running jdk 1.4");
      }
      
      String host = InetAddress.getLocalHost().getHostAddress();

      Main jserver = new Main();
      if (namingBean != null)
      {
         Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
         Method setNamingInfoMethod = jserver.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
         setNamingInfoMethod.invoke(jserver, new Object[] {namingBean});
      }
      jserver.setPort(2410);
      jserver.setBindAddress(host);
      jserver.setRmiPort(31000);
      jserver.start();

   }


   public void run()
   {
      try
      {

         server.unregisterMBean(new ObjectName("remoting:type=JNDIDetector"));
         if (detector != null)
         {
            try
            {
               detector.stop();
            }
            catch (Exception ignored)
            {
            }
         }
         if (connector != null)
         {
            try
            {
               connector.stop();
            }
            catch (Exception ignored)
            {
            }

            connector = null;
            server.unregisterMBean(new ObjectName("jboss.remoting:type=Connector"));
            Thread.sleep(1000);

         }
      }
      catch (Exception e)
      {
      }

   }


   public static void main(String[] args)
   {

      Server server = new Server();
      try
      {
         server.setUp();

         while (true)
         {
            Thread.sleep(1000);
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


   public static class SampleInvocationHandler implements ServerInvocationHandler
   {

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         System.out.println("invoke(): " + invocation.getParameter());
         logger.info("invoke(): " + invocation.getParameter());
         return invocation.getParameter();
//         return null;
      }


      public void addListener(InvokerCallbackHandler callbackHandler)
      {

      }


      public void removeListener(InvokerCallbackHandler callbackHandler)
      {

      }


      public void setMBeanServer(MBeanServer server)
      {

      }

      public void setInvoker(ServerInvoker invoker)
      {

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
}

