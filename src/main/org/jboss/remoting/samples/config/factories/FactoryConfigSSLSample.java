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
package org.jboss.remoting.samples.config.factories;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.samples.config.factories.FactoryConfigSample.CallbackHandler;
import org.jboss.remoting.samples.config.factories.FactoryConfigSample.SampleInvocationHandler;
import org.jboss.remoting.security.SSLServerSocketFactoryService;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketFactoryService;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.w3c.dom.Document;

/**
 * These methods illustrate configuring socket factories and server socket factories
 * on the server side and on the client side.  The numbered
 * options mentioned refer to the lists of configuration options discussed in the
 * Remoting documentation in the subsections "Server side configuration" and
 * "Client side configuration" of the section called "Socket factories and server
 * socket factories".
 * <p>
 * The configuration options illustrated in this class are specific to SSL
 * sockets and server sockets.  The methods
 * <code>getDefaultServerSocketFactory()</code>,
 * <code>getDefaultSocketFactory()</code>,
 * <code>getDefaultCallbackServerSocketFactory()</code> and
 * <code>getDefaultCallbackSocketFactory()</code> 
 * illustrate the use of the class <code>SSLSocketBuilder</code> to
 * create custom SSL socket factories and SSL server socket factories.
 * <p>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) Jul 20, 2006
 * </p>
 */
public class FactoryConfigSSLSample extends TestCase
{
   private static Logger log = Logger.getLogger(FactoryConfigSSLSample.class);
   
   
   
   /** This test illustrates the following set of configuration options:
    * <p>
    * <table border cellpadding="5">
    *  <tr><td align="center"><b>side<td align="center"><b>factory<td><b>option</tr>
    *  <tr><td>server side<td align="center">server socket<td align="center">4</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">4</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">1</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">1</tr>
    * </table>
    */
   public void testFactoriesByPassingMBeanInXml()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
           
         // Create ServerSocketFactory MBean.
         ServerSocketFactory service = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
         mbeanServer.registerMBean(service, objName);
         
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(sconfig);
         mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
         
         // Set xml configuration element.
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append(   "<invoker transport=\"" + getTransport() +"\">");
         buf.append(      "<attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append(      "<attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append(      "<attribute name=\"serverSocketFactory\">" );
         buf.append(          serverSocketFactoryName);
         buf.append(      "</attribute>");
         buf.append(   "</invoker>");
         buf.append("</config>");
         ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
         Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         connector.setConfiguration(xml.getDocumentElement());
         
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         
         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();

         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Create Client.
         Client client = new Client(locator, cconfig);
         
         // Set SocketFactory in Client.
         // Note. There is no provision for using xml configuration on client side.
         SocketFactory sf = getDefaultSocketFactory();
         client.setSocketFactory(sf);
         client.connect();
         System.out.println(getName() + ": " + client.invoke("test invoke()"));

         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////

         // Start callback Connector.
         // Note: there is no provision for using MBeanServer on client side.
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI());
         ServerSocketFactory ssf = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         callbackConnector.start();
         
         // Add callback handler.
         CallbackHandler callbackHandler = new FactoryConfigSample.CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);
         
         client.disconnect();
         callbackConnector.stop();
         connector.stop();
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   
   /** This test illustrates the following set of configuration options:
    * <p>
    * <table border cellpadding="5">
    *  <tr><td align="center"><b>side<td align="center"><b>factory<td><b>option</tr>
    *  <tr><td>server side<td align="center">server socket<td align="center">6</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">5</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">1</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">1</tr>
    * </table>
    * <p>
    * <b>Note.</b>  There is no provision for using an <code>MBeanServer</code> on
    * the client side.  
    */
   public void testFactoriesByPassingMBeanInConfig()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put ServerSocketFactory MBean name in config map.
         ServerSocketFactory service = getDefaultServerSocketFactory();
         String serverSocketFactoryName = "jboss:type=serversocketfactory";
         ObjectName objName = new ObjectName(serverSocketFactoryName);
         MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
         mbeanServer.registerMBean(service, objName);
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, serverSocketFactoryName);
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");

         // Start Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         mbeanServer.registerMBean(connector, new ObjectName("test:type=connector"));
         connector.create();
         connector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         connector.start();

         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Create Client.
         Client client = new Client(locator, cconfig);
         
         // Set server socket factory on Client.
         // Note: There is no provision for using MBeanServer on client side.         
         SocketFactory sf = getDefaultSocketFactory();
         client.setSocketFactory(sf);
         client.connect();
         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         

         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////

         // Start callback Connector.
         // Note: there is no provision for using MBeanServer on client side.
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI());
         ServerSocketFactory ssf = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         callbackConnector.start();
         
         // Add callback handler.
         CallbackHandler callbackHandler = new FactoryConfigSample.CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   
   /** This test illustrates the following set of configuration options:
    * <p>
    * <table border cellpadding="5">
    *  <tr><td align="center"><b>side<td align="center"><b>factory<td><b>option</tr>
    *  <tr><td>server side<td align="center">server socket<td align="center">8</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">8</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">6</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">5</tr>
    * </table>
    */
   public void testFactoriesFromSSLParameters()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL keystore parameters in config map.
         sconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "false");
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
         sconfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
         
         // Start Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         connector.start();
         
         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL parameters in config map.
         cconfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "true");
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         cconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Create Client.
         Client client = new Client(locator, cconfig);
         client.connect();
         System.out.println(getName() + ": " + client.invoke("test invoke()"));
   
         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////

         // Start callback Connector.
         HashMap cbconfig = new HashMap();
         
         // Put SSL parameters in config map.
         cbconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
         cbconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         cbconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         cbconfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), cbconfig);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         callbackConnector.start();
         
         // Add callback handler.
         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);
 
         client.disconnect();
         callbackConnector.stop();
         connector.stop();
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   
   /** This test illustrates the following set of configuration options:
    * <p>
    * <table border cellpadding="5">
    *  <tr><td align="center"><b>side<td align="center"><b>factory<td><b>option</tr>
    *  <tr><td>server side<td align="center">server socket<td align="center">9</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">9</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">7</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">6</tr>
    * </table>
    */
   public void testFactoriesFromSystemSSLParameters()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();

         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Set SSL system properties.
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = getKeystoreFilePath();
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_FILE_PATH, keyStoreFilePath);
         System.setProperty(SSLSocketBuilder.STANDARD_KEY_STORE_PASSWORD, "unit-tests-server");
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = getTruststoreFilePath();
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         System.setProperty(SSLSocketBuilder.STANDARD_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Start Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         connector.start();
         
         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
     
         // Create Client.
         Client client = new Client(locator, cconfig);
         client.connect();
         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         
         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////

         // Start callback Connector.
         HashMap cbconfig = new HashMap();
         
         // Make callback Connector server socket run in client mode.
         cbconfig.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
         
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), cbconfig);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new FactoryConfigSample.SampleInvocationHandler());
         callbackConnector.start();
         
         // Add callback handler.
         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, callbackLocator, callbackHandleObject);

         client.disconnect();
         callbackConnector.stop();
         connector.stop();
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   
   protected String getHostName()
   {
      return "localhost";
   }
   
   
   protected String getTransport()
   {
      return "sslsocket";
   }
   
   
   protected String getKeystoreFilePath()
   {
      File file = new File(FactoryConfigSSLSample.class.getResource("keystore").getFile());
      return file.getPath();
   }
   
   
   protected static String getTruststoreFilePath()
   {
      File file = new File(FactoryConfigSSLSample.class.getResource("truststore").getFile());
      return file.getPath();
   }
   
   
   protected ServerSocketFactory getDefaultServerSocketFactory() throws Exception
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getKeystoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      config.put(SSLSocketBuilder.REMOTING_SSL_PROTOCOL, "SSL");
      SSLSocketBuilder builder = new SSLSocketBuilder(config);
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
      SSLSocketBuilder builder = new SSLSocketBuilder(config);
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
      SSLSocketBuilder builder = new SSLSocketBuilder(config);
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
      SSLSocketBuilder builder = new SSLSocketBuilder(config);
      builder.setUseSSLSocketFactory(false);
      SSLSocketFactoryService service = new SSLSocketFactoryService();
      service.setSSLSocketBuilder(builder);
      service.start();
      return builder.createSSLSocketFactory();
   }
}
