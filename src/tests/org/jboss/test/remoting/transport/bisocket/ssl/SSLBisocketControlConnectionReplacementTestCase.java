/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.test.remoting.transport.bisocket.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.jboss.logging.Logger;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.test.remoting.transport.bisocket.BisocketControlConnectionReplacementTestCase;

/**
 * Unit test for JBREM-1147.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Aug 14, 2009
 * </p>
 */
public class SSLBisocketControlConnectionReplacementTestCase extends BisocketControlConnectionReplacementTestCase
{
   private static Logger log = Logger.getLogger(SSLBisocketControlConnectionReplacementTestCase.class);
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         String keyStoreFilePath = getClass().getResource(".keystore").getFile();
         System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
         System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");
         String trustStoreFilePath = getClass().getResource(".truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
         System.setProperty("javax.net.ssl.trustStorePassword", "unit-tests-client");
      }
      super.setUp();
   }
   
   protected String getTransport()
   {
      return "sslbisocket";
   }
   
   protected String getServerSocketName()
   {
      return SSLTestServerSocketFactory.class.getName();
   }
   
   static public class SSLTestServerSocketFactory extends ServerSocketFactory
   {
      int timeout;
      ServerSocketFactory factory;
      int initialWrites;
      
      public SSLTestServerSocketFactory() throws IOException
      {
         this.timeout = 5000;
         this.initialWrites = INITIAL_WRITES;
         setupFactory();
      }      
      public SSLTestServerSocketFactory(int timeout, int initialWrites) throws IOException
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         setupFactory();
      }
      public ServerSocket createServerSocket() throws IOException
      {
         ServerSocket ss = SSLServerSocketFactory.getDefault().createServerSocket();
         log.info("returning: " + ss);
         return ss;
      }
      public ServerSocket createServerSocket(int port) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = SSLServerSocketFactory.getDefault().createServerSocket(port);
         }
         else
         {
            ss = new SSLTestServerSocket(port, timeout, initialWrites, ((SSLServerSocket) factory.createServerSocket()));
         }
         log.info("returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = SSLServerSocketFactory.getDefault().createServerSocket(port, backlog);
         }
         else
         {
            ss = new SSLTestServerSocket(port, backlog, timeout, initialWrites, ((SSLServerSocket) factory.createServerSocket()));
         }
         log.info("returning: " + ss);
         return ss;
      }

      public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException
      {
         ServerSocket ss = null;
         if (port != secondaryServerSocketPort)
         {
            ss = SSLServerSocketFactory.getDefault().createServerSocket(port, backlog, ifAddress);
         }
         else
         {
            ss = new SSLTestServerSocket(port, backlog, ifAddress, timeout, initialWrites, ((SSLServerSocket) factory.createServerSocket()));
         }
         log.info("returning: " + ss);
         return ss;
      }
      
      protected void setupFactory() throws IOException
      {
         SSLSocketBuilder sslSocketBuilder = new SSLSocketBuilder();
         sslSocketBuilder.setUseSSLServerSocketFactory(false);
         factory = sslSocketBuilder.createSSLServerSocketFactory();
      }
   }
   
   
   static class SSLTestServerSocket extends SSLServerSocket
   {
      int timeout;
      int initialWrites;
      SSLServerSocket serverSocket;

      public SSLTestServerSocket(int timeout, int initialWrites, SSLServerSocket serverSocket) throws IOException
      {
         super();
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.serverSocket = serverSocket;
      }
      public SSLTestServerSocket(int port, int timeout, int initialWrites, SSLServerSocket serverSocket) throws IOException
      {
         super(port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.serverSocket = serverSocket;
         bind(new InetSocketAddress(port), 50);
      }
      public SSLTestServerSocket(int port, int backlog, int timeout, int initialWrites, SSLServerSocket serverSocket) throws IOException
      {
         super(port, backlog);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.serverSocket = serverSocket;
         bind(new InetSocketAddress(port), backlog);
      }
      public SSLTestServerSocket(int port, int backlog, InetAddress bindAddr, int timeout, int initialWrites, SSLServerSocket serverSocket) throws IOException
      {
         super(port, backlog, bindAddr);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.serverSocket = serverSocket;
         bind(new InetSocketAddress(bindAddr, port), 50);
      }
      public Socket accept() throws IOException
      {
         SSLSocket s1 = (SSLSocket) serverSocket.accept();
         Socket s2 = new SSLTestSocket(timeout, initialWrites, s1);
         return s2;
      }
      public void bind(SocketAddress endpoint, int backlog) throws IOException
      {
         log.info("serverSocket: " + serverSocket);
         if (serverSocket != null) log.info("bound: " + serverSocket.isBound());
         if (serverSocket != null && !serverSocket.isBound())
         {
            log.info("binding " + serverSocket);
            serverSocket.bind(endpoint, backlog);
         }
      }
      public String toString()
      {
         return "SSLTestServerSocket[" + serverSocket.toString() + "]";
      }
      public boolean getEnableSessionCreation()
      {
         return serverSocket.getEnableSessionCreation();
      }
      public String[] getEnabledCipherSuites()
      {
         return serverSocket.getEnabledCipherSuites();
      }
      public String[] getEnabledProtocols()
      {
         return serverSocket.getEnabledProtocols();
      }
      public boolean getNeedClientAuth()
      {
         return serverSocket.getNeedClientAuth();
      }
      public String[] getSupportedCipherSuites()
      {
         return serverSocket.getSupportedCipherSuites();
      }
      public String[] getSupportedProtocols()
      {
         return serverSocket.getSupportedProtocols();
      }
      public boolean getUseClientMode()
      {
         return serverSocket.getUseClientMode();
      }
      public boolean getWantClientAuth()
      {
         return serverSocket.getWantClientAuth();
      }
      public void setEnableSessionCreation(boolean arg0)
      {
         serverSocket.setEnableSessionCreation(arg0);
      }
      public void setEnabledCipherSuites(String[] arg0)
      {
         serverSocket.setEnabledCipherSuites(arg0);
      }
      public void setEnabledProtocols(String[] arg0)
      {
         serverSocket.setEnabledProtocols(arg0);
      }
      public void setNeedClientAuth(boolean arg0)
      {
         serverSocket.setNeedClientAuth(arg0);
      }
      public void setUseClientMode(boolean arg0)
      {
         serverSocket.setUseClientMode(arg0);
      }
      public void setWantClientAuth(boolean arg0)
      {
         serverSocket.setWantClientAuth(arg0);
      }
   }

   static class SSLTestSocket extends SSLSocket
   {
      int timeout;
      int initialWrites;
      SSLSocket socket;
      SocketAddress endpoint;
      
      public SSLTestSocket(int timeout, int initialWrites, SSLSocket socket)
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.socket = socket;
      }
      public SSLTestSocket(String host, int port, int timeout, int initialWrites, SSLSocket socket) throws UnknownHostException, IOException
      {
         super(host, port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.socket = socket;
         connect(new InetSocketAddress(host, port), timeout);
      }
      public SSLTestSocket(InetAddress address, int port, int timeout, int initialWrites, SSLSocket socket) throws IOException
      {
         super(address, port);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.socket = socket;
         connect(new InetSocketAddress(address, port), timeout);
      }
      public SSLTestSocket(String host, int port, InetAddress localAddr, int localPort, int timeout, int initialWrites, SSLSocket socket) throws IOException
      {
         super(host, port, localAddr, localPort);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.socket = socket;
         bind(new InetSocketAddress(localAddr, localPort));
         connect(new InetSocketAddress(host, port), timeout);
      }
      public SSLTestSocket(InetAddress address, int port, InetAddress localAddr, int localPort, int timeout, int initialWrites, SSLSocket socket) throws IOException
      {
         super(address, port, localAddr, localPort);
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         this.socket = socket;
         bind(new InetSocketAddress(localAddr, localPort));
         connect(new InetSocketAddress(address, port), timeout);
      }
      public String toString()
      {
         return "SSLTestSocket[" + socket.toString() + "]";
      }
      public InputStream getInputStream() throws IOException
      {
         return socket.getInputStream();
      }
      public OutputStream getOutputStream() throws IOException
      {
         return new TestOutputStream(socket.getOutputStream(), timeout, initialWrites);
      }
      public void addHandshakeCompletedListener(HandshakeCompletedListener listener)
      {
         socket.addHandshakeCompletedListener(listener);
      }
      public void bind(SocketAddress bindpoint) throws IOException
      {
         if (socket != null)
            socket.bind(bindpoint);
      }
      public void connect(SocketAddress endpoint) throws IOException
      {
         if (socket != null)
            socket.connect(endpoint);
      }
      public void connect(SocketAddress endpoint, int timeout) throws IOException
      {
         socket.connect(endpoint, timeout);
      }
      public boolean getEnableSessionCreation()
      {
         return socket.getEnableSessionCreation();
      }
      public String[] getEnabledCipherSuites()
      {
         return socket.getEnabledCipherSuites();
      }
      public String[] getEnabledProtocols()
      {
         return socket.getEnabledProtocols();
      }
      public InetAddress getInetAddress()
      {
         return socket.getInetAddress();
      }
      public boolean getNeedClientAuth()
      {
         return socket.getNeedClientAuth();
      }
      public SSLSession getSession()
      {
         return socket.getSession();
      }
      public String[] getSupportedCipherSuites()
      {
         return socket.getSupportedCipherSuites();
      }
      public String[] getSupportedProtocols()
      {
         return socket.getSupportedProtocols();
      }
      public boolean getUseClientMode()
      {
         return socket.getUseClientMode();
      }
      public boolean getWantClientAuth()
      {
         return socket.getWantClientAuth();
      }
      public void removeHandshakeCompletedListener(HandshakeCompletedListener listener)
      {
         socket.removeHandshakeCompletedListener(listener);
      }
      public void setEnableSessionCreation(boolean flag)
      {
         socket.setEnableSessionCreation(flag);
      }
      public void setEnabledCipherSuites(String[] suites)
      {
         socket.setEnabledCipherSuites(suites);
      }
      public void setEnabledProtocols(String[] protocols)
      {
         socket.setEnabledProtocols(protocols);
      }
      public void setNeedClientAuth(boolean need)
      {
         socket.setNeedClientAuth(need);
      }
      public void setUseClientMode(boolean mode)
      {
         socket.setUseClientMode(mode);
      }
      public void setWantClientAuth(boolean want)
      {
         socket.setWantClientAuth(want);
      }
      public void startHandshake() throws IOException
      {
         socket.startHandshake();
      }
   }
}
