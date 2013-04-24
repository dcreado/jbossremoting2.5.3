/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.transport.config;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.security.CustomSSLServerSocketFactory;
import org.jboss.remoting.security.CustomSSLSocketFactory;
import org.jboss.remoting.security.SSLServerSocketFactoryService;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketBuilderMBean;
import org.jboss.remoting.security.SSLSocketFactoryService;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import org.w3c.dom.Document;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) Jul 20, 2006
 * </p>
 */
public abstract class FactoryConfigTestCaseSSLParent extends FactoryConfigTestCaseParent
{
   public static int secret;
   protected static HashMap initParameters = new HashMap();

   public void testFactoriesByPassingMBeanInXml()
   {
      // There is no specific test for factory identity here, since the ServerSocketFactory
      // MBeans do not expose access to their internal structure.  Instead, we define the
      // default ServerSocketFactorys and SocketFactorys to use the SSL protocol.  Since
      //
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    ////
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         secret = ((short) System.currentTimeMillis()) & 0xffff;

         // Put ServerSocketFactory MBean name in config map.
         ServerSocketFactory service = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         
         MBeanServer mbeanServer = null;
         try
         {
            final ServerSocketFactory finalService = service;
            final ObjectName finalName = objName;
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
                  mbeanServer.registerMBean(finalService, finalName);
                  return mbeanServer;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");

         // Put SSL keystore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");

         // Put SSL truststore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         // Give different protocol than used by default factories.
         sconfig.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "TLS");

         final Connector connector = new Connector(sconfig);
         try
         {
            final MBeanServer finalServer = mbeanServer;
            AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  finalServer.registerMBean(connector, new ObjectName("test:type=connector"));
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }

         // Create another ServerSocketFactoryMBean
         secret = ((short) System.currentTimeMillis()) & 0xffff;
         service = getDefaultServerSocketFactory();
         serverSocketFactoryName = "jboss:type=serversocketfactory2";
         objName = new ObjectName(serverSocketFactoryName);
         
         try
         {
            final MBeanServer finalServer = mbeanServer;
            final ServerSocketFactory finalService = service;
            final ObjectName finalName = objName;
            AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  finalServer.registerMBean(finalService, finalName);
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         
         // Create and set xml configuration document.
         int freeport = PortUtil.findFreePort(getHostName());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"serverSocketFactory\">");
         buf.append(          serverSocketFactoryName);
         buf.append(      "</attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueSocketFactoryClass());
         buf.append(      "</attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
         connector.create();

         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         ServerInvoker serverInvoker = connector.getServerInvoker();
         ServerSocketFactory ssf = serverInvoker.getServerSocketFactory();
         int ssfPort = PortUtil.findFreePort(getHostName());
         ServerSocket ss = ssf.createServerSocket(ssfPort);

         // Verify ServerSocketFactory is the one set in MBeanServer.
         assertEquals(secret, ss.getSoTimeout());


         /////////////////////////////////////////////////
         /////    Make Client. There is no specific   ////
         /////    client test for MBean server case.  ////
         /////////////////////////////////////////////////
         HashMap cconfig = new HashMap();

         // Put SocketFactory in config map.
         SocketFactory sf2 = getDefaultSocketFactory();
         cconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf2);

         // Make Client use remote invoker.
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Put SSL parameters in config map.
         cconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         trustStoreFilePath = getTruststoreFilePath();
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         Client client = new Client(locator, cconfig);
         client.connect();


         //////////////////////////////////////////////
         /////     Do server side callback test.   ////
         //////////////////////////////////////////////
         Thread.sleep(500);
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         HashMap config = new HashMap();
         addExtraCallbackConfig(config);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), config);
         ServerSocketFactory ssf3 = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf3);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         // Verify that callback succeeded.
         assertEquals(1, callbackHandler.getCallbacks().size());

         // Verify callback SocketFactory was derived from ServerSocketFactory in MBeanServer.
         Field field = ServerInvoker.class.getDeclaredField("handlers");
         field.setAccessible(true);
         Map handlers = (Map) field.get(serverInvoker);
         Object obj = handlers.values().iterator().next();
         SampleInvocationHandler sampleInvocationHandler = (SampleInvocationHandler) obj;
         obj = sampleInvocationHandler.getCallbackHandler();
         ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) obj;
         field = ServerInvokerCallbackHandler.class.getDeclaredField("callBackClient");
         field.setAccessible(true);
         Client callbackClient = (Client) field.get(serverInvokerCallbackHandler);
         ClientInvoker callbackClientInvoker = callbackClient.getInvoker();
         SocketFactory sf = callbackClientInvoker.getSocketFactory();

         // Show that callback socket factory comes from SSLServerSocketFactory MBean
         // instead of xml configuration.
         assertTrue(SSLSocketFactoryService.class == sf.getClass());
         Socket s = sf.createSocket(getHostName(), ssfPort);
         assertEquals(secret, s.getSoTimeout());

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
         log.info(getName() + " PASSES");


         connector.stop();
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }


   public void testFactoriesByPassingMBeanInConfigMap()
   {
      // There is no specific test for factory identity here, since the ServerSocketFactory
      // MBeans do not expose access to their internal structure.  Instead, we define the
      // default ServerSocketFactorys and SocketFactorys to use the SSL protocol.  Since
      //
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    ////
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         secret = ((short) System.currentTimeMillis()) & 0xffff;

         // Put ServerSocketFactory MBean name in config map.
         final ServerSocketFactory service = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         final ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = null;

         try
         {
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
                  mbeanServer.registerMBean(service, objName);
                  return mbeanServer;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");

         // Put SSL keystore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");

         // Put SSL truststore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         // Give different protocol than used by default factories.
         sconfig.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "TLS");

         int freeport = PortUtil.findFreePort(getHostName());
         final Connector connector = new Connector(sconfig);
         
         try
         {
            final MBeanServer finalServer = mbeanServer;
            AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  finalServer.registerMBean(connector, new ObjectName("test:type=connector"));
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }

         // Create and set xml configuration document.
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueSocketFactoryClass());
         buf.append(      "</attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());

         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         ServerInvoker serverInvoker = connector.getServerInvoker();
         ServerSocketFactory ssf = serverInvoker.getServerSocketFactory();
         int ssfPort = PortUtil.findFreePort(getHostName());
         ServerSocket ss = ssf.createServerSocket(ssfPort);

         // Verify ServerSocketFactory is the one set in MBeanServer.
         assertEquals(secret, ss.getSoTimeout());


         /////////////////////////////////////////////////
         /////    Make Client. There is no specific   ////
         /////    client test for MBean server case.  ////
         /////////////////////////////////////////////////
         HashMap cconfig = new HashMap();

         // Put SocketFactory in config map.
         SocketFactory sf2 = getDefaultSocketFactory();
         cconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf2);

         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Put SSL parameters in config map.
         cconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         trustStoreFilePath = getTruststoreFilePath();
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Client client = new Client(locator, cconfig);
         client.connect();


         //////////////////////////////////////////////
         /////     Do server side callback test.   ////
         //////////////////////////////////////////////
         Thread.sleep(500);
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         HashMap config = new HashMap();
         addExtraCallbackConfig(config);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), config);
         ServerSocketFactory ssf3 = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf3);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         // Verify that callback succeeded.
         assertEquals(1, callbackHandler.getCallbacks().size());

         // Verify callback SocketFactory was derived from ServerSocketFactory in MBeanServer.
         Field field = ServerInvoker.class.getDeclaredField("handlers");
         field.setAccessible(true);
         Map handlers = (Map) field.get(serverInvoker);
         Object obj = handlers.values().iterator().next();
         SampleInvocationHandler sampleInvocationHandler = (SampleInvocationHandler) obj;
         obj = sampleInvocationHandler.getCallbackHandler();
         ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) obj;
         field = ServerInvokerCallbackHandler.class.getDeclaredField("callBackClient");
         field.setAccessible(true);
         Client callbackClient = (Client) field.get(serverInvokerCallbackHandler);
         ClientInvoker callbackClientInvoker = callbackClient.getInvoker();
         SocketFactory sf = callbackClientInvoker.getSocketFactory();

         // Show that callback socket factory comes from SSLServerSocketFactory MBean
         // instead of xml configuration.
         assertTrue(SSLSocketFactoryService.class == sf.getClass());
         Socket s = sf.createSocket(getHostName(), ssfPort);
         assertEquals(secret, s.getSoTimeout());

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
         log.info(getName() + " PASSES");


         connector.stop();
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }


   public void testFactoriesFromSSLParameters()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    ////
         /////////////////////////////////////
         HashMap sconfig = new HashMap();

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");

         // Put SSL keystore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "false");
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");

         // Put SSL truststore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();

         // Verify ServerSocketFactory was configured according to SSL parameters
         // in config map.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         ServerSocketFactory ssf = serverInvoker.getServerSocketFactory();
         assertTrue(ssf instanceof CustomSSLServerSocketFactory);
         CustomSSLServerSocketFactory csssf = (CustomSSLServerSocketFactory) ssf;
         SSLSocketBuilderMBean builder = csssf.getSSLSocketBuilder();
         assertFalse(builder.isServerSocketUseClientMode());
         assertEquals("JKS", builder.getKeyStoreType());
         File file1 = new File(keyStoreFilePath);
         File file2 = new File(builder.getKeyStore().getFile());
         assertEquals(file1, file2);


         /////////////////////////////////////
         /////    Do client side test.    ////
         /////////////////////////////////////
         HashMap cconfig = new HashMap();

         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Put SSL parameters in config map.
         cconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         trustStoreFilePath = getTruststoreFilePath();
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         Client client = new Client(locator, cconfig);
         client.connect();

         // Verify SocketFactory was configured according to SSL parameters
         // in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         SocketFactory sf = clientInvoker.getSocketFactory();
         assertTrue(sf instanceof CustomSSLSocketFactory);
         CustomSSLSocketFactory cssf = (CustomSSLSocketFactory) sf;
         builder = cssf.getSSLSocketBuilder();
         assertTrue(builder.isSocketUseClientMode());
         assertEquals("JKS", builder.getKeyStoreType());
         file1 = new File(trustStoreFilePath);
         file2 = new File(builder.getTrustStore().getFile());
         assertEquals(file1, file2);

         //////////////////////////////////////////////
         /////     Do server side callback test.   ////
         //////////////////////////////////////////////
         Thread.sleep(500);
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         HashMap config = new HashMap();
         addExtraCallbackConfig(config);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), config);
         ServerSocketFactory ssf3 = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf3);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         // Verify that callback succeeded.
         assertEquals(1, callbackHandler.getCallbacks().size());

         // Verify SocketFactory was configured according to SSL parameters in config map.
         Field field = ServerInvoker.class.getDeclaredField("handlers");
         field.setAccessible(true);
         Map handlers = (Map) field.get(serverInvoker);
         Object obj = handlers.values().iterator().next();
         SampleInvocationHandler sampleInvocationHandler = (SampleInvocationHandler) obj;
         obj = sampleInvocationHandler.getCallbackHandler();
         ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) obj;
         field = ServerInvokerCallbackHandler.class.getDeclaredField("callBackClient");
         field.setAccessible(true);
         Client callbackClient = (Client) field.get(serverInvokerCallbackHandler);
         ClientInvoker callbackClientInvoker = callbackClient.getInvoker();
         sf = callbackClientInvoker.getSocketFactory();
         assertTrue(sf instanceof CustomSSLSocketFactory);
         cssf = (CustomSSLSocketFactory) sf;
         builder = cssf.getSSLSocketBuilder();
         assertFalse(builder.isSocketUseClientMode());
         assertEquals("JKS", builder.getKeyStoreType());
         file1 = new File(keyStoreFilePath);
         file2 = new File(builder.getKeyStore().getFile());
         assertEquals(file1, file2);

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }


   public void testFactoriesFromSystemSSLParameters()
   {
      try
      {
         /////////////////////////////////////
         /////    Start Connector.        ////
         /////////////////////////////////////
         // There are no specific ServerSocketFactory tests here, since different
         // transports handle the default case differently.
         HashMap sconfig = new HashMap();

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");

         // Set SSL system properties.
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_FILE_PATH, keyStoreFilePath);
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_PASSWORD, "unit-tests-server");
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_PASSWORD, "unit-tests-client");

         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();


         /////////////////////////////////////
         /////    Do client side test.    ////
         /////////////////////////////////////
         // There are no specific SocketFactory tests here, since different
         // transports handle the default case differently.
         HashMap cconfig = new HashMap();

         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         Client client = new Client(locator, cconfig);
         client.connect();

         // Verify that invocation works.
         client.invoke("abc");

         //////////////////////////////////////////////
         /////     Do server side callback test.   ////
         //////////////////////////////////////////////
         Thread.sleep(500);
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         HashMap config = new HashMap();
         addExtraCallbackConfig(config);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), config);
         ServerSocketFactory ssf3 = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf3);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         // Verify that callback succeeded.
         assertEquals(1, callbackHandler.getCallbacks().size());

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }


   protected ServerSocketFactory getDefaultServerSocketFactory() throws Exception
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getKeystoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "SSL");
      SSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, secret);
      builder.setUseSSLServerSocketFactory(false);
      SSLServerSocketFactoryService service = new SSLServerSocketFactoryService();
      service.setSSLSocketBuilder(builder);
      service.start();
      return service;
   }

   protected SocketFactory getDefaultSocketFactory() throws Exception
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getTruststoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "SSL");
      SSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, secret);
      builder.setUseSSLSocketFactory(false);
      SSLSocketFactoryService service = new SSLSocketFactoryService();
      service.setSSLSocketBuilder(builder);
      service.start();
      return service;
   }

   protected ServerSocketFactory getDefaultCallbackServerSocketFactory() throws Exception
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getTruststoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "SSL");
      SSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, secret);
      builder.setUseSSLServerSocketFactory(false);
      SSLServerSocketFactoryService service = new SSLServerSocketFactoryService();
      service.setSSLSocketBuilder(builder);
      service.start();
      return service;
   }

   protected SocketFactory getDefaultCallbackSocketFactory() throws Exception
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getKeystoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "SSL");
      SSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, secret);
      builder.setUseSSLSocketFactory(false);
      SSLSocketFactoryService service = new SSLSocketFactoryService();
      service.setSSLSocketBuilder(builder);
      service.start();
      return builder.createSSLSocketFactory();
   }

   protected Class getUniqueServerSocketFactoryClass() throws Exception
   {
      initParameters.put(UniqueServerSocketFactory.class, getDefaultServerSocketFactory());
      return UniqueServerSocketFactory.class;
   }

   protected Class getUniqueSocketFactoryClass() throws Exception
   {
      initParameters.put(UniqueSocketFactory.class, getDefaultSocketFactory());
      return UniqueSocketFactory.class;
   }

   protected Class getUniqueCallbackServerSocketFactoryClass() throws Exception
   {
      initParameters.put(UniqueCallbackServerSocketFactory.class, getDefaultCallbackServerSocketFactory());
      return UniqueCallbackServerSocketFactory.class;
   }

   protected Class getUniqueCallbackSocketFactoryClass() throws Exception
   {
      initParameters.put(UniqueCallbackSocketFactory.class, getDefaultCallbackSocketFactory());
      return UniqueCallbackSocketFactory.class;
   }

   public static class SelfIdentifyingSSLSocketBuilder
   extends SSLSocketBuilder implements Serializable
   {
      public int identity;

      public SelfIdentifyingSSLSocketBuilder(int identity)
      {
         super();
         this.identity = identity;
      }

      public SelfIdentifyingSSLSocketBuilder(Map config, int identity)
      {
         super(config);
         this.identity = identity;
      }

      public SocketFactory createSSLSocketFactory() throws IOException
      {
         SocketFactory sf = super.createSSLSocketFactory();
         return new SelfIdentifyingSocketFactory(sf, identity);
      }

      public ServerSocketFactory createSSLServerSocketFactory() throws IOException
      {
         ServerSocketFactory ssf = super.createSSLServerSocketFactory();
         return new SelfIdentifyingServerSocketFactory(ssf, identity);
      }
   }

   public static class SelfIdentifyingServerSocketFactory extends SSLServerSocketFactory
   {
      private ServerSocketFactory ssf;
      public int identity;

      public SelfIdentifyingServerSocketFactory(ServerSocketFactory ssf, int identity)
      {
         this.ssf = ssf;
         this.identity = identity;
         log.info("identity: " + identity);
      }

      public ServerSocket createServerSocket(int arg0) throws IOException
      {
         ServerSocket ss = ssf.createServerSocket(arg0);
         ss.setSoTimeout(identity);
         return ss;
      }

      public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
      {
         return ssf.createServerSocket(arg0, arg1);
      }

      public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
      {
         return ssf.createServerSocket(arg0, arg1, arg2);
      }

      public String[] getDefaultCipherSuites()
      {
         return null;
      }

      public String[] getSupportedCipherSuites()
      {
         return null;
      }
   }

   public static class SelfIdentifyingSocketFactory extends SSLSocketFactory implements Serializable
   {
      private SocketFactory sf;
      public int identity;

      public SelfIdentifyingSocketFactory(SocketFactory sf, int identity)
      {
         this.sf = sf;
         this.identity = identity;
      }

      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         Socket s = sf.createSocket(arg0, arg1);
         s.setSoTimeout(identity);
         return s;
      }
      
      public Socket createSocket() throws IOException
      {
         return sf.createSocket();
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         return sf.createSocket(arg0, arg1);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException
      {
         return ((SSLSocketFactory)sf).createSocket(arg0, arg1, arg2, arg3);
      }

      public String[] getDefaultCipherSuites()
      {
         return ((SSLSocketFactory)SSLSocketFactory.getDefault()).getDefaultCipherSuites();
      }

      public String[] getSupportedCipherSuites()
      {
         return ((SSLSocketFactory)SSLSocketFactory.getDefault()).getSupportedCipherSuites();
      }
   }

   public static class UniqueServerSocketFactory extends SelfIdentifyingServerSocketFactory
   {
      public UniqueServerSocketFactory()
      {
         super((ServerSocketFactory) initParameters.get(UniqueServerSocketFactory.class), 0);
      }
   }

   public static class UniqueSocketFactory extends SelfIdentifyingSocketFactory
   {
      public UniqueSocketFactory()
      {
         super((SocketFactory) initParameters.get(UniqueSocketFactory.class), 0);
      }
   }

   public static class UniqueCallbackServerSocketFactory extends SelfIdentifyingServerSocketFactory
   {
      public UniqueCallbackServerSocketFactory()
      {
         super((ServerSocketFactory) initParameters.get(UniqueCallbackServerSocketFactory.class), 0);
      }
   }

   public static class UniqueCallbackSocketFactory extends SelfIdentifyingSocketFactory
   {
      public UniqueCallbackSocketFactory()
      {
         super((SocketFactory) initParameters.get(UniqueCallbackSocketFactory.class), 0);
      }
   }
}
