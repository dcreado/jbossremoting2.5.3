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
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.jboss.remoting.transport.ClientInvoker;
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
 * The configuration options illustrated in this class are applicable to any kind
 * of socket and server socket, so the <code>SampleServerSocketFactory</code>
 * and <code>SampleSocketFactory</code> classes create ordinary sockets and server
 * sockets.
 * <p>
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) Jul 20, 2006
 * </p>
 */
public class FactoryConfigSample extends TestCase
{
   protected static Logger log = Logger.getLogger(FactoryConfigSample.class);
  
   
   /**
    * This test illustrates the following set of configuration options:
    * <p>
    * <table border cellpadding="5">
    *  <tr><td align="center"><b>side<td align="center"><b>factory<td><b>option</tr>
    *  <tr><td>server side<td align="center">server socket<td align="center">1</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">1</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">1</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">1</tr>
    * </table>
    */
   public void testFactoriesBySettingInvokers()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         
         // Set ServerSocketFactory and SocketFactory in ServerInvoker.
         ServerInvoker serverInvoker = connector.getServerInvoker();
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         serverInvoker.setServerSocketFactory(ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         serverInvoker.setSocketFactory(sf1);

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
         client.connect();
         
         // Set SocketFactory in ClientInvoker.
         SocketFactory sf2 = getDefaultSocketFactory();
         ClientInvoker clientInvoker = client.getInvoker();
         clientInvoker.setSocketFactory(sf2);

         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         
         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////

         // Start callback Connector.
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI());
         callbackConnector.create();
         ServerInvoker callbackServerInvoker = callbackConnector.getServerInvoker();
         ServerSocketFactory ssf2 = getDefaultCallbackServerSocketFactory();
         callbackServerInvoker.setServerSocketFactory(ssf2);
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
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
    *  <tr><td>server side<td align="center">server socket<td align="center">2</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">2</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">2</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">2</tr>
    * </table>
    */
   public void testFactoriesBySettingConnectorAndClient()
   {
      try
      {
         /////////////////////////////////////
         /////     Set up server side.    //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         
         // Set ServerSocketFactory and SocketFactory in Connector.
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         connector.setServerSocketFactory(ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         connector.setSocketFactory(sf1);
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
         SocketFactory sf2 = getDefaultSocketFactory();
         client.setSocketFactory(sf2);
         client.connect();
         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         
         
         //////////////////////////////////////////////
         /////      Set up callback handling.      //// 
         //////////////////////////////////////////////

         // Get callback Connector.
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI());
        
         // Set ServerSocketFactory in callback Connector
         ServerSocketFactory ssf2 = getDefaultCallbackServerSocketFactory();
         callbackConnector.setServerSocketFactory(ssf2);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
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
    *  <tr><td>server side<td align="center">server socket<td align="center">3</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">3</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">3</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">3</tr>
    * </table>
    */
   public void testFactoriesByPassingInConfig()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put ServerSocketFactory and SocketFactory in config map.
         ServerSocketFactory ssf1 = getDefaultServerSocketFactory();
         sconfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf1);
         SocketFactory sf1 = getDefaultCallbackSocketFactory();
         sconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf1);
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         
         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory in config map.
         SocketFactory sf2 = getDefaultSocketFactory();
         cconfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sf2);
         
         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Create Client.
         Client client = new Client(locator, cconfig);
         client.connect();

         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         
         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////
         
         // Get callback Connector.
         HashMap cbconfig = new HashMap();
         ServerSocketFactory ssf2 = getDefaultCallbackServerSocketFactory();
         cbconfig.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf2);
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), cbconfig);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
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
    *  <tr><td>server side<td align="center">server socket<td align="center">5</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">6</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">4</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">1</tr>
    * </table>
    */
   public void testFactoriesByPassingClassnameInXml()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
           
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(sconfig);
         
         // Set xml configuration element.
         StringBuffer buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append(   "<invoker transport=\"" + getTransport() +"\">");
         buf.append(      "<attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append(      "<attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append(      "<attribute name=\"serverSocketFactory\">" );
         buf.append(          getDefaultServerSocketFactoryClass().getName());
         buf.append(      "</attribute>");
         buf.append(      "<attribute name=\"socketFactory\">" );
         buf.append(         getDefaultSocketFactoryClass().getName());
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
         
         // Get callback Connector.
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector();
         
         // Set xml configuration element.
         buf = new StringBuffer();
         buf.append("<?xml version=\"1.0\"?>\n");
         buf.append("<config>");
         buf.append(   "<invoker transport=\"" + getTransport() +"\">");
         buf.append(      "<attribute name=\"serverBindAddress\">" + getHostName() + "</attribute>");
         buf.append(      "<attribute name=\"serverBindPort\">" + freeport + "</attribute>");
         buf.append(      "<attribute name=\"serverSocketFactory\">" );
         buf.append(         getDefaultCallbackServerSocketFactoryClass().getName());
         buf.append(      "</attribute>");
         buf.append(   "</invoker>");
         buf.append("</config>");
         bais = new ByteArrayInputStream(buf.toString().getBytes());
         xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
         callbackConnector.setConfiguration(xml.getDocumentElement());
         
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
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
    *  <tr><td>server side<td align="center">server socket<td align="center">7</tr>
    *  <tr><td>server side<td align="center">socket       <td align="center">7</tr>
    *  <tr><td>client side<td align="center">server socket<td align="center">5</tr>
    *  <tr><td>client side<td align="center">socket       <td align="center">4</tr>
    * </table>
    */
   public void testFactoriesByClassNameinConfig()
   {
      try
      {
         /////////////////////////////////////
         /////    Set up server side.     //// 
         /////////////////////////////////////
         HashMap sconfig = new HashMap();
         
         // Put class names of ServerSocketFactory and SocketFactory in config map.
         sconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, getDefaultServerSocketFactoryClass().getName());
         sconfig.put(Remoting.SOCKET_FACTORY_NAME, getDefaultCallbackSocketFactoryClass().getName());
         
         // Make callback Client use remote invoker.
         sconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Get Connector.
         int freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector connector = new Connector(locator, sconfig);
         connector.create();
         connector.addInvocationHandler("sample", new SampleInvocationHandler());
         connector.start();
         
         
         /////////////////////////////////////
         /////    Set up client side.     //// 
         /////////////////////////////////////
         HashMap cconfig = new HashMap();
         
         // Put SocketFactory class name in config map.
         cconfig.put(Remoting.SOCKET_FACTORY_NAME, getDefaultSocketFactoryClass().getName());
         
         // Make Client use remote invoker.
         cconfig.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Create Client.
         Client client = new Client(locator, cconfig);
         client.connect();

         System.out.println(getName() + ": " + client.invoke("test invoke()"));
         
         
         //////////////////////////////////////////////
         /////       Set up callback handling.     //// 
         //////////////////////////////////////////////
         
         // Get callback Connector.
         HashMap cbconfig = new HashMap();
         cbconfig.put(ServerInvoker.SERVER_SOCKET_FACTORY, getDefaultCallbackServerSocketFactoryClass().getName());
         freeport = PortUtil.findFreePort(getHostName());
         InvokerLocator callbackLocator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
         Connector callbackConnector = new Connector(callbackLocator.getLocatorURI(), cbconfig);
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
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
         
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected String getHostName()
   {
      return "localhost";
   }
   

   protected ServerSocketFactory getDefaultServerSocketFactory() throws Exception
   {
      return new SampleServerSocketFactory();
   }
   
   
   protected SocketFactory getDefaultSocketFactory() throws Exception
   {
      return new SampleSocketFactory();
   }
   
   
   protected ServerSocketFactory getDefaultCallbackServerSocketFactory() throws Exception
   {
      return new SampleServerSocketFactory();
   }
   
   
   protected SocketFactory getDefaultCallbackSocketFactory() throws Exception
   {
      return new SampleSocketFactory();
   }
   
   
   protected Class getDefaultServerSocketFactoryClass() throws Exception
   {
      return SampleServerSocketFactory.class;
   }
   
   
   protected Class getDefaultSocketFactoryClass() throws Exception
   {
      return SampleSocketFactory.class;
   }
   
   
   protected Class getDefaultCallbackServerSocketFactoryClass() throws Exception
   {
      return SampleServerSocketFactory.class;
   }
   
   
   protected Class getDefaultCallbackSocketFactoryClass() throws Exception
   {
      return SampleSocketFactory.class;
   }
   
   
   public static class SampleServerSocketFactory
   extends ServerSocketFactory
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
   
   
   public static class SampleSocketFactory
   extends SocketFactory
   {
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
   
   
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler callbackHandler;
      
      public SampleInvocationHandler()
      {   
      }
      
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
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
      /**
       * Will take the callback and print out its values.
       *
       * @param callback
       * @throws org.jboss.remoting.callback.HandleCallbackException
       *
       */
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("Received callback value of: " + callback.getCallbackObject());
      }
   }
}