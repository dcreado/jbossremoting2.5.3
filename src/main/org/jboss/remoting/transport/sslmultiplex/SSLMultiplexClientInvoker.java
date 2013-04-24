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

/*
 * Created on Mar 24, 2006
 */
package org.jboss.remoting.transport.sslmultiplex;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.transport.multiplex.Multiplex;
import org.jboss.remoting.transport.multiplex.MultiplexClientInvoker;
import org.jboss.remoting.transport.multiplex.VirtualSocket;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;


/**
 * <code>SSLMultiplexClientInvoker</code> is the client side of the sslmultiplex transport.
 * For more information, see Remoting documentation on labs.jboss.org.
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2188 $
 * <p>
 * Copyright (c) 2006
 * </p>
 */

public class SSLMultiplexClientInvoker extends MultiplexClientInvoker
{
   private static final Logger log = Logger.getLogger(SSLMultiplexClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();
   

   public SSLMultiplexClientInvoker(InvokerLocator locator) throws IOException
   {
      super(locator);
   }


/**
 * @param locator
 * @param configuration
 * @throws IOException
 */
   public SSLMultiplexClientInvoker(InvokerLocator locator, Map configuration) throws IOException
   {
      super(locator, configuration);
   }


/**
 *
 */
   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory sf = super.createSocketFactory(configuration);

      if (isCompleteSocketFactory(sf))
      {
         socketFactory = sf;
         return sf;
      }
      
      SocketFactory wrapper = sf;

      try
      {
         SSLSocketBuilder server = new SSLSocketBuilder(configuration);
         server.setUseSSLSocketFactory(false);
         sf = server.createSSLSocketFactory();
         this.configuration.put(Multiplex.SOCKET_FACTORY, sf);
      }
      catch(Exception e)
      {
         log.error("Error creating SSL Socket Factory for client invoker.", e);
      }

      if (wrapper != null)
      {
         ((SocketFactoryWrapper) wrapper).setSocketFactory(sf);
         socketFactory = wrapper;
         return wrapper;
      }
      
      socketFactory = sf;
      return sf;
   }


   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      SocketFactory sf = getSocketFactory();
      if (sf == null)
         createSocketFactory(configuration);

      VirtualSocket socket = new VirtualSocket(configuration);
      socket.connect(getConnectSocketAddress(), getBindSocketAddress(), this.timeout);
      return socket;
   }
}

