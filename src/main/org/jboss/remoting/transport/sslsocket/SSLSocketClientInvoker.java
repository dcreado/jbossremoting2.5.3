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
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.util.socket.HandshakeRepeater;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketClientInvoker extends SocketClientInvoker
{
   private static final Logger log = Logger.getLogger(SSLSocketClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();
   
   protected String[] enabledCipherSuites;
   protected String[] enabledProtocols;
   protected boolean enableSessionCreation = true;

   public SSLSocketClientInvoker(InvokerLocator locator) throws IOException
   {
      super(locator);
      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.debug("Error setting up ssl socket client invoker.", ex);
         throw new RuntimeException(ex.getMessage());
      }
   }

   public SSLSocketClientInvoker(InvokerLocator locator, Map configuration) throws IOException
   {
      super(locator, configuration);
      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.debug("Error setting up ssl socket client invoker.", ex);
         throw new RuntimeException(ex.getMessage());
      }
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
   
   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory sf = super.createSocketFactory(configuration);

      if (isCompleteSocketFactory(sf))
      {
         return sf;
      }

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
         socketFactory = wrapper;
         return wrapper;
      }

      return sf;
   }


   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      SocketFactory sf = getSocketFactory();

      if (sf == null)
         sf = createSocketFactory(configuration);

      Socket s = sf.createSocket();

      configureSocket(s);
      InetSocketAddress inetAddr = new InetSocketAddress(address, port);
      
      if (timeout < 0)
      {
         timeout = getTimeout();
         if (timeout < 0)
            timeout = 0;
      }
      
      connect(s, inetAddr, timeout);

      if (s instanceof SSLSocket)
      {
         // need to check for handshake listener and add them if there is one
         Object obj = configuration.get(Client.HANDSHAKE_COMPLETED_LISTENER);
         if (obj != null && obj instanceof HandshakeCompletedListener)
         {
            SSLSocket sslSocket = (SSLSocket) s;
            HandshakeCompletedListener listener = (HandshakeCompletedListener) obj;
            establishHandshake(sslSocket, listener);
         }
      }

      return s;
   }
   
   protected void configureSocket(Socket s) throws SocketException
   {
      s.setReuseAddress(getReuseAddress());
      
      if (keepAliveSet)           s.setKeepAlive(keepAlive);
      if (receiveBufferSize > -1) s.setReceiveBufferSize(receiveBufferSize);
      if (sendBufferSize > -1)    s.setSendBufferSize(sendBufferSize);
      if (soLingerSet && 
            soLingerDuration > 0) s.setSoLinger(soLinger, soLingerDuration);
      if (trafficClass > -1)      s.setTrafficClass(trafficClass);
      
      if (s instanceof SSLSocket)
      {
         SSLSocket ss = (SSLSocket) s;
         if (enabledCipherSuites != null)
         {
            ss.setEnabledCipherSuites(enabledCipherSuites);
         }
         if (enabledProtocols != null)
         {
            ss.setEnabledProtocols(enabledProtocols);
         }
         ss.setEnableSessionCreation(enableSessionCreation);
      }
   }

   private void establishHandshake(SSLSocket sslSocket, HandshakeCompletedListener listener)
         throws IOException
   {
      HandshakeRepeater repeater = new HandshakeRepeater(listener);
      sslSocket.addHandshakeCompletedListener(repeater);
      sslSocket.getSession();
      repeater.waitForHandshake();
   }
   
   static private void connect(final Socket socket, final InetSocketAddress address, final int timeout)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         socket.connect(address, timeout);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               socket.connect(address, timeout);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }   
   }
}