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

package org.jboss.remoting.transport.sslsocket;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketServerInvoker extends SocketServerInvoker implements SSLSocketServerInvokerMBean
{
   private static final Logger log = Logger.getLogger(SSLSocketServerInvoker.class);

   protected String[] enabledCipherSuites;
   protected String[] enabledProtocols;
   protected boolean enableSessionCreation = true;
   
   public SSLSocketServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public SSLSocketServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
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
   
   protected ServerSocketFactory getDefaultServerSocketFactory()
   {
      return SSLServerSocketFactory.getDefault();
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