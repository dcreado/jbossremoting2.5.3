/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.transport.socket.timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;


/**
 * Unit tests for JBREM-1120.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Apr 22, 2009
 * </p>
 */
public abstract class SSLWriteTimeoutTestParent extends WriteTimeoutTestParent
{
   private static Logger log = Logger.getLogger(SSLWriteTimeoutTestParent.class);
   
   private static boolean firstTime = true;
   
   protected static int SECONDARY_SERVER_SOCKET_PORT = 8765;
   protected static String SECONDARY_SERVER_SOCKET_PORT_STRING = "8765";
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         String keyStoreFilePath = getClass().getResource("../.keystore").getFile();
         System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
         System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");
         String trustStoreFilePath = getClass().getResource("../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
         System.setProperty("javax.net.ssl.trustStorePassword", "unit-tests-client");
      }
      super.setUp();
   }
   
   
   protected String getServerSocketFactoryClassName()
   {
      return SSLTestServerSocketFactory.class.getName();
   }
   
   protected Constructor getServerSocketFactoryConstructor() throws NoSuchMethodException
   {
      return SSLTestServerSocketFactory.class.getConstructor(new Class[]{int.class, int.class});
   }
   
   protected String getSocketFactoryClassName()
   {
      return SSLTestSocketFactory.class.getName();
   }
   
   protected Constructor getSocketFactoryConstructor() throws NoSuchMethodException
   {
      return SSLTestSocketFactory.class.getConstructor(new Class[]{int.class, int.class});
   }
   
   static public class SSLTestServerSocketFactory extends ServerSocketFactory
   {
      int timeout;
      ServerSocketFactory factory;
      int initialWrites;
      
      public SSLTestServerSocketFactory() throws IOException
      {
         this.timeout = 5000;
         this.initialWrites = -1;
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
         ServerSocket ss = null;
         if (callbackTest)
         {
            ss = SSLServerSocketFactory.getDefault().createServerSocket();
         }
         else
         {
            ss = new SSLTestServerSocket(timeout, initialWrites, ((SSLServerSocket) factory.createServerSocket()));
         }
         log.info("returning: " + ss);
         return ss;
      }
      public ServerSocket createServerSocket(int port) throws IOException
      {
         ServerSocket ss = null;
         if (callbackTest && port != secondaryServerSocketPort)
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
         if (callbackTest && port != secondaryServerSocketPort)
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
         if (callbackTest && port != secondaryServerSocketPort)
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
   
   
   public static class SSLTestSocketFactory extends SocketFactory
   {
      int timeout;
      int initialWrites;
      SocketFactory factory;
      
      public SSLTestSocketFactory() throws IOException
      {
         timeout = 5000;
         initialWrites = -1;
         setupFactory();
      }
      public SSLTestSocketFactory(int timeout, int initialWrites) throws IOException
      {
         this.timeout = timeout;
         this.initialWrites = initialWrites;
         setupFactory();
      }
      public Socket createSocket() throws IOException
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest)
         {
            s = factory.createSocket();
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
            s = new SSLTestSocket(timeout, initialWrites, ((SSLSocket) factory.createSocket()));
         }
         log.info(this + " returning " + s);
         return s;
      }
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = factory.createSocket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
            s = new SSLTestSocket(arg0, arg1, timeout, initialWrites, ((SSLSocket) factory.createSocket()));
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = factory.createSocket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
            s = new SSLTestSocket(arg0, arg1, timeout, initialWrites, ((SSLSocket) factory.createSocket()));
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = factory.createSocket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
            s = new SSLTestSocket(arg0, arg1, arg2, arg3, timeout, initialWrites, ((SSLSocket) factory.createSocket()));
         }
         log.info(this + " returning " + s);
         return s;
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         log.info("callbackTest: " + callbackTest);
         Socket s = null;
         if (callbackTest && arg1 != secondaryServerSocketPort)
         {
            s = factory.createSocket(arg0, arg1);
         }
         else
         {
            s = new TestSocket(timeout, initialWrites);
            s = new SSLTestSocket(arg0, arg1, arg2, arg3, timeout, initialWrites, ((SSLSocket) factory.createSocket()));
         }
         log.info(this + " returning " + s);
         return s;
      }
      
      protected void setupFactory() throws IOException
      {
         SSLSocketBuilder sslSocketBuilder = new SSLSocketBuilder();
         sslSocketBuilder.setUseSSLServerSocketFactory(false);
         factory = sslSocketBuilder.createSSLSocketFactory();
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