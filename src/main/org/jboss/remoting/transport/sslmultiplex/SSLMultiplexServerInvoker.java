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

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.CustomSSLServerSocketFactory;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketBuilderMBean;
import org.jboss.remoting.transport.multiplex.MultiplexServerInvoker;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.util.Map;


/**
 * <code>SSLMultiplexServerInvoker</code> is the server side of the sslmultiplex transport.
 * For more information, see Remoting documentation on labs.jboss.org.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1248 $
 * <p>
 * Copyright (c) Mar 24, 2006
 * </p>
 */

public class SSLMultiplexServerInvoker extends MultiplexServerInvoker
{

/**
 * @param locator
 */
   public SSLMultiplexServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }


/**
 * @param locator
 * @param configuration
 */
   public SSLMultiplexServerInvoker(InvokerLocator locator, Map configuration) throws IOException
   {
      super(locator, configuration);
   }

/**
 * @param configuration
 * @return
 */
   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory socketFactory = null;
      if ((socketFactory = super.createSocketFactory(configuration)) != null)
         return socketFactory;

      ServerSocketFactory serverSocketFactory = getServerSocketFactory();

      try
      {
         if (serverSocketFactory  instanceof CustomSSLServerSocketFactory)
         {
            CustomSSLServerSocketFactory customServerSocketFactory = (CustomSSLServerSocketFactory) serverSocketFactory;
            SSLSocketBuilderMBean builder = customServerSocketFactory.getSSLSocketBuilder();
            boolean shouldUseDefault = builder.getUseSSLServerSocketFactory();
            builder.setUseSSLSocketFactory(shouldUseDefault);
            boolean useClientMode = builder.isServerSocketUseClientMode();
            builder.setSocketUseClientMode(useClientMode);
            return builder.createSSLSocketFactory();
         }

         SSLSocketBuilder builder = new SSLSocketBuilder(configuration);
         return builder.createSSLSocketFactory();
      }
      catch(Exception e)
      {
         log.error("Error creating SSL Socket Factory for server invoker.", e);
//         throw new IOException("Error creating SSL Socket Factory.  Root cause: " + e.getMessage());
         return null;
      }
   }

   protected ServerSocketFactory getDefaultServerSocketFactory()
   {
      return SSLServerSocketFactory.getDefault();
   }
}

