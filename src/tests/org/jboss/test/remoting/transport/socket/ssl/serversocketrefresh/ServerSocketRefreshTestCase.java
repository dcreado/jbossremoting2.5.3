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
package org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * ServerSocketRefreshTestCase replaces Michael Voss' test
 * org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh.TestCase.
 * It tests the same functionality, namely, the hot replacement of a ServerSocket
 * in a socket transport server.  However, it is easier to manage the possibility
 * that it may take a while to rebind a ServerSocket to an old port.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 9, 2008
 * </p>
 */
public class ServerSocketRefreshTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ServerSocketRefreshTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testServerSocketRefresh() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();

      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStorePath = getClass().getResource("certificate/clientTrustStore").getFile();
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStorePath);
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "testpw");
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");
      clientConfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStorePath = getClass().getResource("certificate/clientKeyStore").getFile();
      clientConfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStorePath);
      clientConfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "testpw");
      clientConfig.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");

      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      client.disconnect();
      
      // Get current ServerSocket.
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("acceptThreads");
      field.setAccessible(true);
      SocketServerInvoker.AcceptThread[] threads = null;
      threads = (SocketServerInvoker.AcceptThread[]) field.get(invoker);
      assertEquals(1, threads.length);
      ServerSocket serverSocket = threads[0].getServerSocket();
      log.info("original ServerSocket: " + serverSocket);
      
      // Update ServerSocket.
      String trustStorePath2 = getClass().getResource("certificate/serverTrustStore2").getFile();
      ServerSocketFactory ssf = createServerSocketFactory("testpw", "testpw", keyStorePath, trustStorePath2);
      invoker.setNewServerSocketFactory(ssf);
      log.info("passed in new ServerSocketFactory");
      
      Thread.sleep(10000);
      int i = 0;
      while (true)
      {
         if (!serverSocket.equals(threads[0].getServerSocket()))
            break;

         if (++i >= 10)
            break;
         
         log.info("ServerSocket has not been replaced yet. Will wait 30 seconds and try again.");
         Thread.sleep(30000);
      }

      // Verify new Client is unable to carry out invocation.
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ServerSocket newServerSocket = threads[0].getServerSocket();
      log.info("new ServerSocket: " + newServerSocket);
      assertNotSame(newServerSocket, serverSocket);
      boolean success = false;
      
      try
      {
         System.out.println("*****************************************************************");
         System.out.println("******************  EXCEPTIONS EXPECTED  ************************");
         System.out.println("*****************************************************************");
         client.invoke("xyz");
      }
      catch (CannotConnectException e)
      {
         log.info("got expected exception:" + e.getMessage());
         success = true;
      }
      catch (Throwable t)
      {
         log.error("got unexpected exception: ", t);
      }
      
      assertTrue("expected exception", success);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "sslsocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStorePath = getClass().getResource("certificate/serverKeyStore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStorePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "testpw");
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_ALGORITHM, "SunX509");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStorePath = getClass().getResource("certificate/serverTrustStore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStorePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "testpw");
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_ALGORITHM, "SunX509");
      ServerSocketFactory ssf = createServerSocketFactory("testpw", "testpw", keyStorePath, trustStorePath);
      config.put(Remoting.CUSTOM_SERVER_SOCKET_FACTORY, ssf);
      config.put("reuseAddress", "true");
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   /**
    * returns a SSLServerSocketFactory that requires a client certificate to build up SSL connection
    * @param keyStorePassword
    * @param trustStorePassword
    * @param keyStorePath
    * @param trustStorePath
    * @return SSLServerSocketFactory
    * @throws Exception
    */
   public SSLServerSocketFactory createServerSocketFactory(String keyStorePassword,
                                                           String trustStorePassword,
                                                           String keyStorePath,
                                                           String trustStorePath)
   throws Exception
   {                
      SSLServerSocketFactory ssf = createNoAuthServerSocketFactory(keyStorePassword,trustStorePassword,keyStorePath,trustStorePath);
      return new ClientAuthSocketFactory(ssf);
   }
   
   
   /**
    * returns a SSLServerSocketFactory
    * @param keyStorePassword
    * @param trustStorePassword
    * @param keyStorePath
    * @param trustStorePath
    * @return SSLServerSocketFactory
    * @throws Exception
    */
   public static SSLServerSocketFactory createNoAuthServerSocketFactory(String keyStorePassword, String trustStorePassword, String keyStorePath, String trustStorePath) throws Exception
   {       
       FileInputStream stream=null;
       try {
           // create an SSLContext
           SSLContext context = null;

           context = SSLContext.getInstance("TLS");

           // define password
           char[] keyPassphrase = keyStorePassword.toCharArray();
           char[] trustPassphrase = trustStorePassword.toCharArray();
           
           // load the server key store
           KeyStore server_keystore = KeyStore.getInstance("JKS");
           stream = new FileInputStream(keyStorePath);
           server_keystore.load(stream, keyPassphrase);
           stream.close();
           
           // load the server trust store
           KeyStore server_truststore = KeyStore.getInstance("JKS");
           stream = new FileInputStream(trustStorePath);
           server_truststore.load(stream, trustPassphrase);
           stream.close();
           
           // initialize a KeyManagerFactory with the KeyStore
           KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
           kmf.init(server_keystore, keyPassphrase);           
           // KeyManagers from the KeyManagerFactory
           KeyManager[] keyManagers = kmf.getKeyManagers();

           // initialize a TrustManagerFactory with the TrustStore
           TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
           tmf.init(server_truststore);
           // TrustManagers from the TrustManagerFactory
           TrustManager[] trustManagers = tmf.getTrustManagers();

           // initialize context with Keystore and Truststore information
           context.init(keyManagers, trustManagers, null);

           // get ServerSocketFactory from context
           return context.getServerSocketFactory();
           
       } catch (Exception e) {
           throw e;
       } finally {
           try {
               stream.close();
           } catch (Exception ioe) {
           }
       }
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   /**
    * overrides createServerSocket methods from class SSLServerSocketFactory<br>
    * sets NeedClientAuth true, so server asks for a client certificate in the SSL handshake
    * @author <a href="mailto:michael.voss@hp.com">Michael Voss</a>
    *
    */
   public static class ClientAuthSocketFactory extends SSLServerSocketFactory{
       SSLServerSocketFactory serverSocketFactory;

       /**
        * @param serverSocketFactory
        */
       public ClientAuthSocketFactory(SSLServerSocketFactory serverSocketFactory)
       {
          this.serverSocketFactory = serverSocketFactory;
       }

       public ServerSocket createServerSocket() throws IOException
       {
          SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket();
          ss.setNeedClientAuth(true);
          return ss;
       }

       public ServerSocket createServerSocket(int arg0) throws IOException
       {
          SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0);
          ss.setNeedClientAuth(true);
          return ss;
       }

       public ServerSocket createServerSocket(int arg0, int arg1) throws IOException
       {
          SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0, arg1);
          ss.setNeedClientAuth(true);
          return ss;
       }

       public ServerSocket createServerSocket(int arg0, int arg1, InetAddress arg2) throws IOException
       {
          SSLServerSocket ss = (SSLServerSocket) serverSocketFactory.createServerSocket(arg0, arg1, arg2);
          ss.setNeedClientAuth(true);
          return ss;
         
       }

       public boolean equals(Object obj)
       {
          return serverSocketFactory.equals(obj);
       }

       public String[] getDefaultCipherSuites()
       {
          return serverSocketFactory.getDefaultCipherSuites();
       }

       public String[] getSupportedCipherSuites()
       {
          return serverSocketFactory.getSupportedCipherSuites();
       }

       public int hashCode()
       {
          return serverSocketFactory.hashCode();
       }

       public String toString()
       {
          return serverSocketFactory.toString();
       }
   }
}