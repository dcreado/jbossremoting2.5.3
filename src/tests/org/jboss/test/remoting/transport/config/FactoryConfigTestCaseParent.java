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

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.security.SocketFactoryMBean;
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
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3873 $
 * <p>
 * Copyright (c) Jun 13, 2006
 * </p>
 */
public abstract class FactoryConfigTestCaseParent extends TestCase
{
   protected static Logger log = Logger.getLogger(FactoryConfigTestCaseParent.class);
   protected static boolean firstTime = true;
   
   
   abstract protected String getTransport();
   
   
   public static String getKeystoreFilePath()
   {
      File dir = (File)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return  new File(FactoryConfigTestCaseParent.class.getResource(".").getFile());
         }
      });
      while (!"transport".equals(dir.getName()))
      {
         dir = new File(dir.getParent());
      }
      return dir.getPath() + File.separator + "config/.keystore";
   }
   
   
   public static String getTruststoreFilePath()
   {
      File dir = new File(FactoryConfigTestCaseParent.class.getResource(".").getFile());
      while (!"transport".equals(dir.getName()))
      {
         dir = new File(dir.getParent());
      }
      return dir.getPath() + File.separator + "config/.truststore";
   }
   
   
   public void setUp()
   {
      if (firstTime)
      {
         firstTime = false;
         log.info("********************************************************");
         log.info("*********** FactoryConfigTestCase: " + getTransport() + " ***********");
         log.info("********************************************************");
      }
   }
   
  
   public void testFactoriesBySettingInvokers()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put ServerSocketFactory and SocketFactory in config map.
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         sconfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         sconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf1);
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Put ServerSocketFactory MBean name in config map.
         ServerSocketFactory serverSocketService = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = null;
         try
         {
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return MBeanServerFactory.createMBeanServer();
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         mbeanServer.registerMBean(serverSocketService, objName);
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);
         
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
         
         Connector connector = new Connector(sconfig);
         mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
         
         // Create and set xml configuration document.
         int freeport = PortUtil.findFreePort(getHostName());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"serverSocketFactory\">");
         buf.append(         getUniqueServerSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
         connector.create();
         
         // Set ServerSocketFactory and SocketFactory in ServerInvoker.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         ServerSocketFactory ssf2 = getDefaultServerSocketFactory();
         serverInvoker.setServerSocketFactory(ssf2);
         SocketFactory sf2 = getDefaultCallbackSocketFactory();
         serverInvoker.setSocketFactory(sf2);

         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();

         // Verify ServerSocketFactory is the one set in ServerInvoker.
         assertTrue(ssf2 == serverInvoker.getServerSocketFactory());
         
         
         /////////////////////////////////////
         /////    Do client side test.    //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory in config map.
         SocketFactory sf3 = getDefaultSocketFactory();
         cconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf3);
         
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
         
         // Set SocketFactory in ClientInvoker.
         SocketFactory sf4 = getDefaultSocketFactory();
         ClientInvoker clientInvoker = client.getInvoker();
         clientInvoker.setSocketFactory(sf4);
         
         // Verify SocketFactory is the one set in ClientInvoker.
         assertTrue(sf4 == clientInvoker.getSocketFactory());
         
         
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
         
         // Verify callback SocketFactory is the one set in SocketInvoker.
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
         assertTrue(sf2 == callbackClientInvoker.getSocketFactory());
         
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
   
   
   public void testFactoriesBySettingConnectorAndClient()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put ServerSocketFactory and SocketFactory in config map.
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         sconfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         sconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf1);
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Put ServerSocketFactory MBean name in config map.
         ServerSocketFactory serverSocketService = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = null;
         try
         {
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return MBeanServerFactory.createMBeanServer();
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         mbeanServer.registerMBean(serverSocketService, objName);
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);
         
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
         
         Connector connector = new Connector(sconfig);
         mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
         
         // Create and set xml configuration document.
         int freeport = PortUtil.findFreePort(getHostName());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"serverSocketFactory\">");
         buf.append(         getUniqueServerSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
                  
         // Set ServerSocketFactory and SocketFactory in Connector.
         ServerSocketFactory ssf2 = getDefaultServerSocketFactory();
         connector.setServerSocketFactory(ssf2);
         SocketFactory sf2 = getDefaultCallbackSocketFactory();
         connector.setSocketFactory(sf2);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         
         // Verify ServerSocketFactory is the one set in Connector.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         assertTrue(ssf2 == serverInvoker.getServerSocketFactory());
         
         
         /////////////////////////////////////
         /////    Do client side test.    //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory in config map.
         SocketFactory sf3 = getDefaultSocketFactory();
         cconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf3);
         
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
         
         // Set SocketFactory in Client.
         SocketFactory sf4 = getDefaultSocketFactory();
         client.setSocketFactory(sf4);
         client.connect();
         
         // Verify SocketFactory is the one set in Client.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(sf4 == clientInvoker.getSocketFactory());
         
         
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
         
         // Verify callback SocketFactory is the one set in Connector.
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
         assertTrue(sf2 == callbackClientInvoker.getSocketFactory());
         
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

   
   public void testFactoriesByPassingInConfig()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put ServerSocketFactory and SocketFactory in config map.
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         sconfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         sconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf1);
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Put ServerSocketFactory MBean name in config map.
         ServerSocketFactory serverSocketService = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = null;

         try
         {
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return MBeanServerFactory.createMBeanServer();
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
             
         mbeanServer.registerMBean(serverSocketService, objName);
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);
         
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
         
         Connector connector = new Connector(sconfig);
         mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
         
         // Create and set xml configuration document.
         int freeport = PortUtil.findFreePort(getHostName());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"serverSocketFactory\">");
         buf.append(         getUniqueServerSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueSocketFactoryClass());
         buf.append("      </attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
         
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();

         // Verify ServerSocketFactory is the one passed in config map.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         assertTrue(ssf1 == serverInvoker.getServerSocketFactory());
         
         
         /////////////////////////////////////
         /////    Do client side test.    //// 
         /////////////////////////////////////
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

         // Verify SocketFactory is the one passed in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(sf2 == clientInvoker.getSocketFactory());
         
         
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
         
         // Verify callback SocketFactory is the one passed in config map.
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
         assertTrue(sf1 == callbackClientInvoker.getSocketFactory());
         
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
   
   
   public void testFactoriesByClassNameInXmlDoc()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put class names of ServerSocketFactory and SocketFactory in config map.
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, ServerSocketFactory.getDefault().getClass().getName());
         sconfig.put(Remoting.SOCKET_FACTORY_NAME, SocketFactory.getDefault().getClass().getName());
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Special case: tell HTTPSClientInvoker to ignore hostname in certificates.
         // This is because InvokerLocator turns "localhost" into "127.0.0.1". Should
         // be fixed by JBREM-497.
         sconfig.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Put ServerSocketFactory MBean name in config map.
         final ServerSocketFactory serverSocketService = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         final ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = null;
         try
         {
            mbeanServer = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return MBeanServerFactory.createMBeanServer();
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
         try
         {
            final MBeanServer finalServer = mbeanServer;
            AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  finalServer.registerMBean(serverSocketService, objName);
                  return null;
               }
            });
         }
         catch (PrivilegedActionException e)
         {
            throw (Exception) e.getCause();
         }
//         mbeanServer.registerMBean(serverSocketService, objName);
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);
         
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
         
         Connector connector = new Connector(sconfig);
         
         // Create and set xml configuration document.
         int freeport = PortUtil.findFreePort(getHostName());
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append("   <invoker transport=\"" + getTransport() + "\">");
         buf.append("      <attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append("      <attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append("      <attribute name=\"serverSocketFactory\">" +
                              getUniqueServerSocketFactoryClass().getName() +
                          "</attribute>");
         buf.append("      <attribute name=\"socketFactory\">");
         buf.append(         getUniqueCallbackSocketFactoryClass().getName());
         buf.append(      "</attribute>");
         buf.append("   </invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
         
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();

         // Verify ServerSocketFactory is the one passed in config map.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         assertTrue(getUniqueServerSocketFactoryClass() ==
                    serverInvoker.getServerSocketFactory().getClass());
         
         
         /////////////////////////////////////
         /////    Do client side test.    //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory class name in config map.
         cconfig.put(Remoting.SOCKET_FACTORY_NAME, getUniqueSocketFactoryClass().getName());
         
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

         // Verify SocketFactory is the one passed in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(getUniqueSocketFactoryClass() == clientInvoker.getSocketFactory().getClass());
         
         
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
         
         // Verify callback SocketFactory is the one passed in config map.
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
         assertTrue(getUniqueCallbackSocketFactoryClass() ==
                    callbackClientInvoker.getSocketFactory().getClass());
         
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
   
   
   public void testFactoriesByClassNameInConfigMap()
   {
      try
      {
         /////////////////////////////////////
         /////    Do server side test.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put class names of ServerSocketFactory and SocketFactory in config map.
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, getUniqueServerSocketFactoryClass().getName());
         sconfig.put(Remoting.SOCKET_FACTORY_NAME, getUniqueCallbackSocketFactoryClass().getName());
         
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
         
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();

         // Verify ServerSocketFactory is the one passed in config map.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         assertTrue(getUniqueServerSocketFactoryClass() ==
                    serverInvoker.getServerSocketFactory().getClass());
         
         
         /////////////////////////////////////
         /////    Do client side test.    //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory class name in config map.
         cconfig.put(Remoting.SOCKET_FACTORY_NAME, getUniqueSocketFactoryClass().getName());
         
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

         // Verify SocketFactory is the one passed in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(getUniqueSocketFactoryClass() == clientInvoker.getSocketFactory().getClass());
         
         
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
         
         // Verify callback SocketFactory is the one passed in config map.
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
         assertTrue(getUniqueCallbackSocketFactoryClass() ==
                    callbackClientInvoker.getSocketFactory().getClass());
         
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
   
   
   protected String getHostName()
   {
      return "localhost";
   }
   

   protected ServerSocketFactory getDefaultServerSocketFactory() throws Exception
   {
      return new SelfIdentifyingServerSocketFactory();
   }
   
   
   protected SocketFactory getDefaultSocketFactory() throws Exception
   {
      return SocketFactory.getDefault();
   }
   
   
   protected ServerSocketFactory getDefaultCallbackServerSocketFactory() throws Exception
   {
      return new SelfIdentifyingServerSocketFactory();
   }
   
   
   protected SocketFactory getDefaultCallbackSocketFactory() throws Exception
   {
      return SocketFactory.getDefault();
   }
   
   protected Class getUniqueServerSocketFactoryClass() throws Exception
   {
      return UniqueServerSocketFactory.class;
   }
   
   protected Class getUniqueSocketFactoryClass() throws Exception
   {
      return UniqueSocketFactory.class;
   }
   
   protected Class getUniqueCallbackServerSocketFactoryClass() throws Exception
   {
      return UniqueServerSocketFactory.class;
   }
   
   protected Class getUniqueCallbackSocketFactoryClass() throws Exception
   {
      return UniqueSocketFactory.class;
   }
   
   protected void addExtraCallbackConfig(Map config)
   {
   }
   
   public static class UniqueServerSocketFactory
   extends ServerSocketFactory
   implements ServerSocketFactoryMBean
   {
      public ServerSocket createServerSocket(int arg0) throws IOException
      {
         return new ServerSocket(arg0);
      }

      public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
      {
         return new ServerSocket(arg0, arg1);
      }

      public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
      {
         return new ServerSocket(arg0, arg1, arg2);
      }
   }
   
   public static class UniqueSocketFactory
   extends SocketFactory
   implements SocketFactoryMBean
   {
      public Socket createSocket() throws IOException
      {
         return new Socket();
      }
      
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         return new Socket(arg0, arg1);
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         return new Socket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         return new Socket(arg0, arg1);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         return new Socket(arg0, arg1, arg2, arg3);
      }
   }
   
   public interface SelfIdentifyingServerSocketFactoryMBean extends ServerSocketFactoryMBean
   {
      ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException;
      ServerSocket createServerSocket(int port, int backlog) throws IOException;
      ServerSocket createServerSocket(int port) throws IOException;
      ServerSocket createServerSocket() throws IOException;
      long getIdentity();
      void setIdentity(long identity);
   }
   
   
   public static class SelfIdentifyingServerSocketFactory
   extends ServerSocketFactory
   implements SelfIdentifyingServerSocketFactoryMBean
   {  
      private long identity;
      
      public SelfIdentifyingServerSocketFactory()
      {
      }
      
      public long getIdentity()
      {
         return identity;
      }
      
      public void setIdentity(long identity)
      {
         this.identity = identity;
      }
      
      public ServerSocket createServerSocket(int arg0) throws IOException
      {
         return new SelfIdentifyingServerSocket(arg0, identity);
      }

      public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
      {
         return new SelfIdentifyingServerSocket(arg0, arg1, identity);
      }

      public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
      {
         return new SelfIdentifyingServerSocket(arg0, arg1, arg2, identity);
      }
      
      class SelfIdentifyingServerSocket extends ServerSocket
      {
         private long identity;
         
         public SelfIdentifyingServerSocket(int port, int backlog, InetAddress bindAddr, long identity)
         throws IOException
         {
            super(port, backlog, bindAddr);
            this.identity = identity;
         }

         public SelfIdentifyingServerSocket(int port, int backlog, long identity) throws IOException
         {
            super(port, backlog);
            this.identity = identity;
         }

         public SelfIdentifyingServerSocket(int port, long identity) throws IOException
         {
            super(port);
            this.identity = identity;
         }

         public SelfIdentifyingServerSocket(long identity) throws IOException
         {
            super();
            this.identity = identity;
         }
         
         public long getIdentity()
         {
            return identity;
         }
      }
   }
   
   
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler callbackHandler;
      
      public SampleInvocationHandler()
      {   
      }
      
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return new Integer(0);
      }
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         log.info("entering addListener()");
         this.callbackHandler = callbackHandler;
         
         try
         {
            Callback callback = new Callback(new Integer(1));
            callbackHandler.handleCallback(callback);
            log.info("sent first callback");
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }
      
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }
      
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }
      
      public InvokerCallbackHandler getCallbackHandler()
      {
         return callbackHandler;
      }
   }
   
   
   public static class CallbackHandler implements InvokerCallbackHandler
   {
      private ArrayList callbacks = new ArrayList();
      
      public CallbackHandler()
      {
      }
      
      public ArrayList getCallbacks()
      {
         return callbacks;
      }
      
      /**
       * Will take the callback and print out its values.
       *
       * @param callback
       * @throws org.jboss.remoting.callback.HandleCallbackException
       *
       */
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("Received push callback.");
         log.info("Received callback value of: " + callback.getCallbackObject());
         log.info("Received callback server invoker of: " + callback.getServerLocator());
         callbacks.add(callback);
      }
   }
}