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

import org.jboss.logging.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.rmi.RemotingRMIClientSocketFactory;
import org.jboss.remoting.util.socket.HandshakeRepeater;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3626 $
 * <p>
 * Copyright (c) Jun 9, 2006
 * </p>
 */
public class SerializableSSLClientSocketFactory extends RemotingRMIClientSocketFactory
{
   private static final long serialVersionUID = 3242156275483606618L;
   private static Logger log = Logger.getLogger(SerializableSSLClientSocketFactory.class);


   public SerializableSSLClientSocketFactory(InvokerLocator invokerLocator,
                                             int timeout,
                                             Map configuration)
   {
      super(invokerLocator, invokerLocator.getHost(), timeout, configuration);
      this.invokerLocator = invokerLocator;
      this.configuration = new HashMap(configuration);
   }


   public Socket createSocket(String host, int port) throws IOException
   {
      Socket s = super.createSocket(host, port);

      // need to check for handshake listener and add them if there is one
      Object obj = configuration.get(Client.HANDSHAKE_COMPLETED_LISTENER);
      if (obj != null && obj instanceof HandshakeCompletedListener)
      {
         SSLSocket sslSocket = (SSLSocket) s;
         HandshakeCompletedListener listener = (HandshakeCompletedListener) obj;
         establishHandshake(sslSocket, listener);
      }

      return s;
   }


   public SocketFactory retrieveSocketFactory(ComparableHolder holder) throws IOException
   {
      SocketFactory sf = (SocketFactory) socketFactories.get(this);
      if (sf == null)
      {
         try
         {
            // We want to keep the local configuration map, which might contain a
            // SocketFactory, separate from the configuration map, which is meant
            // to contain only serializable objects.
            Map tempConfig = new HashMap(configuration);
            Map localConfig = (Map) configMaps.get(holder);
            if (localConfig != null)
               tempConfig.putAll(localConfig);

            if (tempConfig.containsKey(Remoting.CUSTOM_SOCKET_FACTORY))
            {
               sf = (SocketFactory) tempConfig.get(Remoting.CUSTOM_SOCKET_FACTORY);
            }
            else
            {
               SSLSocketBuilder socketBuilder = new SSLSocketBuilder(tempConfig);
               socketBuilder.setUseSSLSocketFactory( false );
               sf = socketBuilder.createSSLSocketFactory();
               sf = AbstractInvoker.wrapSocketFactory(sf, tempConfig);
            }

            socketFactories.put(this, sf);

            // Get handshake listener from local configuration map and store in
            // configuration map brought over from server.  We don't have to worry
            // about the handshake listener being serializable because if we
            // find an entry in configMaps, we are running on the client.
            if (localConfig != null)
            {
               Object obj = localConfig.get(Client.HANDSHAKE_COMPLETED_LISTENER);
               if (obj != null)
                  configuration.put(Client.HANDSHAKE_COMPLETED_LISTENER, obj);
            }
         }
         catch (IOException e)
         {
            log.debug(e);
            throw new RuntimeException("Unable to create customized SSL socket factory", e);
         }
      }
      
      return sf;
   }
   
   
   private void establishHandshake(SSLSocket sslSocket, HandshakeCompletedListener listener)
         throws IOException
   {
      HandshakeRepeater repeater = new HandshakeRepeater(listener);
      sslSocket.addHandshakeCompletedListener(repeater);
      sslSocket.getSession();
      repeater.waitForHandshake();
   }
}