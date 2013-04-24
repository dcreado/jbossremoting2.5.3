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

package org.jboss.remoting.transport.sslrmi;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.rmi.RMIServerInvoker;
import org.jboss.remoting.transport.rmi.RemotingRMIClientSocketFactory;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3661 $
 * <p>
 * Copyright (c) Jun 9, 2006
 * </p>
 */
public class SSLRMIServerInvoker extends RMIServerInvoker
{
   public SSLRMIServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }


   public SSLRMIServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected RemotingRMIClientSocketFactory getRMIClientSocketFactory(String ignored)
   {
      // Remove from config map any properties relating to keystore and truststore.
      HashMap remoteConfig = new HashMap(configuration);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_ALIAS);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_PASSWORD);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD);
      remoteConfig.remove(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE);
      
      // Remove ServerSocketFactory.
      remoteConfig.remove(Remoting.CUSTOM_SERVER_SOCKET_FACTORY);
      
      // Remove server side socket creation listeners.
      remoteConfig.remove(Remoting.SOCKET_CREATION_CLIENT_LISTENER);
      remoteConfig.remove(Remoting.SOCKET_CREATION_SERVER_LISTENER);

      // If server socket should use client mode, then default behavior will be for socket
      // to not use client mode.
      String serverSocketUseClientModeString
         = (String) configuration.get(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE);
      if (serverSocketUseClientModeString != null)
      {
         boolean serverSocketUseClientMode = Boolean.valueOf(serverSocketUseClientModeString).booleanValue();
         if (serverSocketUseClientMode)
         {
            String socketUseClientModeString
               = (String) configuration.get(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE);
            if (socketUseClientModeString == null)
            {
               remoteConfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
            }
         }
      }

      return new SerializableSSLClientSocketFactory(locator, getTimeout(), remoteConfig);
   }


   protected ServerSocketFactory getDefaultServerSocketFactory() throws IOException
   {
      SSLSocketBuilder builder = new SSLSocketBuilder(configuration);
      builder.setUseSSLServerSocketFactory(false);
      return builder.createSSLServerSocketFactory();
   }
   
   
   protected SocketFactory createSocketFactory(Map configuration)
   {
      return null;
   }
}
