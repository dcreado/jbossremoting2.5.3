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

package org.jboss.remoting.transport.sslbisocket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.sslsocket.SSLSocketServerInvokerMBean;
import org.jboss.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketServerInvoker
   extends BisocketServerInvoker
   implements SSLSocketServerInvokerMBean
{

   private static final Logger log = Logger.getLogger(SSLBisocketServerInvoker.class);
   
   protected String[] enabledCipherSuites;
   protected String[] enabledProtocols;
   protected boolean enableSessionCreation = true;
   
   public SSLBisocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public SSLBisocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected ServerSocketFactory getDefaultServerSocketFactory()
   {
      return SSLServerSocketFactory.getDefault();
   }
   
   public void setOOBInline(boolean inline)
   {
      log.warn("Ignored: sending urgent data is not supported by SSLSockets");
   }
   
   public String[] getEnabledCipherSuites()
   {
      return enabledCipherSuites;
   }

   public void setEnabledCipherSuites(String[] enabledCipherSuites)
   {
      this.enabledCipherSuites = enabledCipherSuites;
   }

   public String[] getEnabledProtocols()
   {
      return enabledProtocols;
   }

   public void setEnabledProtocols(String[] enabledProtocols)
   {
      this.enabledProtocols = enabledProtocols;
   }

   public boolean isEnableSessionCreation()
   {
      return enableSessionCreation;
   }

   public void setEnableSessionCreation(boolean enableSessionCreation)
   {
      this.enableSessionCreation = enableSessionCreation;
   }

   protected ServerSocketFactory createServerSocketFactory() throws IOException
   {
      if (isCallbackServer)
         return null;
      
      return super.createServerSocketFactory();
   }
   
   protected void setup() throws Exception
   {
      super.setup();
      
      Object o = configuration.get("enabledCipherSuites");
      if (o instanceof String[])
         setEnabledCipherSuites((String[]) o);
      
      o = configuration.get("enabledProtocols");
      if (o instanceof String[])
         setEnabledProtocols((String[]) o);
   }
   
   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory sf = super.createSocketFactory(configuration);

      if (isCompleteSocketFactory(sf))
         return sf;
      
      SocketFactory wrapper = sf;

      try
      {
         SSLSocketBuilder server = new SSLSocketBuilder(configuration);
         sf = server.createSSLSocketFactory();
      }
      catch (Exception e)
      {
         log.error("Error creating SSL Socket Factory for client invoker: " + e.getMessage());
         log.debug("Error creating SSL Socket Factory for client invoker.", e);
      }

      if (wrapper != null)
      {
         ((SocketFactoryWrapper) wrapper).setSocketFactory(sf);
         return wrapper;
      }
      
      return sf;
   }


   protected void configureServerSocket(ServerSocket ss) throws SocketException
   {
      super.configureServerSocket(ss);
      
      if (ss instanceof SSLServerSocket)
      {
         SSLServerSocket sss = (SSLServerSocket) ss;
         if (enabledCipherSuites != null)
         {
            sss.setEnabledCipherSuites(enabledCipherSuites);
         }
         if (enabledProtocols != null)
         {
            sss.setEnabledProtocols(enabledProtocols);
         }
         sss.setEnableSessionCreation(enableSessionCreation);
      }
   }
}