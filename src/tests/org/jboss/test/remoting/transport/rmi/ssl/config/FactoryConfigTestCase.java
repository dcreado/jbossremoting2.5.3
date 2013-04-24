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
package org.jboss.test.remoting.transport.rmi.ssl.config;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.remoting.security.SSLServerSocketFactoryServiceMBean;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketBuilderMBean;
import org.jboss.test.remoting.transport.config.FactoryConfigTestCaseSSLParent;

/**
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) May 20, 2006
 * </p>
 */
public class FactoryConfigTestCase extends FactoryConfigTestCaseSSLParent
{
   protected String getTransport()
   {
      return "sslrmi";
   }
   
   // Note.
   // RMI ServerSocketFactorys aren't required to be Serializable, but for the
   // tests we put them in config maps that get serialized.
   protected ServerSocketFactory getDefaultServerSocketFactory() throws IOException
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getKeystoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      return new SerializableServerSocketFactory(config);
   }
   
   protected ServerSocketFactory getDefaultCallbackServerSocketFactory() throws IOException
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getTruststoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      return new SerializableServerSocketFactory(config);
   }
   
   protected SocketFactory getDefaultSocketFactory() throws IOException
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getTruststoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      return new SerializableSocketFactory(config);
   }
   
   protected SocketFactory getDefaultCallbackSocketFactory() throws IOException
   {
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = getKeystoreFilePath();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      return new SerializableSocketFactory(config);
   }
   
   public interface SerializableServerSocketFactoryMBean
   extends SSLServerSocketFactoryServiceMBean
   {
      public abstract ServerSocket createServerSocket(int arg0) throws IOException;
      public abstract ServerSocket createServerSocket(int arg0, int arg1) throws IOException;
      public abstract ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException;
   }
   
   public static class SerializableServerSocketFactory extends ServerSocketFactory
   implements Serializable, SerializableServerSocketFactoryMBean
   {
      Map config;
      
      public SerializableServerSocketFactory(Map config)
      {
         this.config = config;
      }

      /* (non-Javadoc)
       * @see org.jboss.test.remoting.transport.rmi.ssl.config.SerializableServerSocketFactoryMBean#createServerSocket(int)
       */
      public ServerSocket createServerSocket(int arg0) throws IOException
      {
         int identity = FactoryConfigTestCaseSSLParent.secret;
         SelfIdentifyingSSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, identity);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
            builder.setServerSocketUseClientMode(true);
         builder.setUseSSLServerSocketFactory(false);
         return builder.createSSLServerSocketFactory().createServerSocket(arg0);
      }

      /* (non-Javadoc)
       * @see org.jboss.test.remoting.transport.rmi.ssl.config.SerializableServerSocketFactoryMBean#createServerSocket(int, int)
       */
      public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
      {
         int identity = FactoryConfigTestCaseSSLParent.secret;
         SelfIdentifyingSSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, identity);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
            builder.setServerSocketUseClientMode(true);
         builder.setUseSSLServerSocketFactory(false);
         return builder.createSSLServerSocketFactory().createServerSocket(arg0, arg1);
      }

      /* (non-Javadoc)
       * @see org.jboss.test.remoting.transport.rmi.ssl.config.SerializableServerSocketFactoryMBean#createServerSocket(int, int, java.net.InetAddress)
       */
      public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
      {
         int identity = FactoryConfigTestCaseSSLParent.secret;
         SelfIdentifyingSSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, identity);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
            builder.setServerSocketUseClientMode(true);
         builder.setUseSSLServerSocketFactory(false);
         return builder.createSSLServerSocketFactory().createServerSocket(arg0, arg1, arg2);
      }

      public void create() throws Exception
      {
      }

      public void start() throws Exception
      { 
      }

      public void stop()
      {
      }

      public void destroy()
      {
      }

      public void setSSLSocketBuilder(SSLSocketBuilderMBean sslSocketBuilder)
      {
      }

      public SSLSocketBuilderMBean getSSLSocketBuilder()
      {
         int identity = FactoryConfigTestCaseSSLParent.secret;
         SelfIdentifyingSSLSocketBuilder builder = new SelfIdentifyingSSLSocketBuilder(config, identity);
         builder.setUseSSLServerSocketFactory(false);
         return builder;
      }
   }
   
   static class SerializableSocketFactory extends SocketFactory implements Serializable
   {
      Map config;
      
      public SerializableSocketFactory(Map config)
      {
         this.config = config;
      }
      
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         SSLSocketBuilder builder = new SSLSocketBuilder(config);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
            builder.setSocketUseClientMode(false);
         builder.setUseSSLSocketFactory(false);
         return builder.createSSLSocketFactory().createSocket(arg0, arg1);
      }
      
      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         SSLSocketBuilder builder = new SSLSocketBuilder(config);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
            builder.setSocketUseClientMode(false);
         builder.setUseSSLSocketFactory(false);
         return builder.createSSLSocketFactory().createSocket(arg0, arg1, arg2, arg3);
      }
      
      
      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         SSLSocketBuilder builder = new SSLSocketBuilder(config);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
            builder.setSocketUseClientMode(false);
         builder.setUseSSLSocketFactory(false);
         return builder.createSSLSocketFactory().createSocket(arg0, arg1);
      }
      
      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         SSLSocketBuilder builder = new SSLSocketBuilder(config);
         if (config.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
            builder.setSocketUseClientMode(false);
         builder.setUseSSLSocketFactory(false);
         return builder.createSSLSocketFactory().createSocket(arg0, arg1, arg2, arg3);
      }
   }
}
