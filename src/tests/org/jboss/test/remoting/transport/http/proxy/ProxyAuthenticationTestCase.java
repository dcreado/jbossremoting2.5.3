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
package org.jboss.test.remoting.transport.http.proxy;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.util.Base64;


/**
 * Unit test for JBREM-1051.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Oct 29, 2008
 * </p>
 */
public class ProxyAuthenticationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ProxyAuthenticationTestCase.class);
   
   private static boolean firstTime = true;
   private static String syspropAuth  = "Basic " + Base64.encodeBytes("sysprop:abc".getBytes());
   private static String metadataAuth = "Basic " + Base64.encodeBytes("metadata:xyz".getBytes());
   
   protected TestHttpServer server;

   
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
      };
   }
   
   
   /**
    * Tests behavior with proxy and authorization information stored in system properties.
    */
   public void testProxySyspropAuthSysprop() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Set system properties.
      System.setProperty("http.proxyHost", server.host);
      System.setProperty("http.proxyPort", Integer.toString(server.port));
      System.setProperty("proxySet", "true");
      System.setProperty("http.proxy.username", "sysprop");
      System.setProperty("http.proxy.password", "abc");
      
      // Create invocation metadata map.
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      
      // Run test.
      doTest(metadata, syspropAuth);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with proxy information stored in system properties
    * and authorization information stored in system properties and in overriding
    * invocation metadata map..
    */
   public void testProxySyspropAuthMeta() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Set system properties.
      setSystemProperty("http.proxyHost", server.host);
      setSystemProperty("http.proxyPort", Integer.toString(server.port));
      setSystemProperty("proxySet", "true");
      setSystemProperty("http.proxy.username", "sysprop");
      setSystemProperty("http.proxy.password", "abc");
      
      // Create invocation metadata map.
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("http.proxy.username", "metadata");
      metadata.put("http.proxy.password", "xyz");
      
      // Run test.
      doTest(metadata, metadataAuth);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with proxy information stored in invocation metadata map
    * and authorization information stored in system properties.
    */
   public void testProxyMetaAuthSysprop() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Set system properties.
      setSystemProperty("http.proxy.username", "sysprop");
      setSystemProperty("http.proxy.password", "abc");   
      
      // Create invocation metadata map.
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("http.proxyHost", server.host);
      metadata.put("http.proxyPort", Integer.toString(server.port));
      
      // Run test.
      doTest(metadata, syspropAuth);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with proxy information stored in invocation metadata map
    * and authorization information stored in system properties and overriding
    * invocation metadata map.
    */
   public void testProxyMetaAuthMeta() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Set system properties.
      setSystemProperty("http.proxy.username", "sysprop");
      setSystemProperty("http.proxy.password", "abc");   
      
      // Create invocation metadata map.
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("http.proxyHost", server.host);
      metadata.put("http.proxyPort", Integer.toString(server.port));
      metadata.put("http.proxy.username", "metadata");
      metadata.put("http.proxy.password", "xyz");
      
      // Run test.
      doTest(metadata, metadataAuth);
      log.info(getName() + " PASSES");
   }
   
   
   protected void setupServer() throws Exception
   {
      server = new TestHttpServer();
      server.start();
      synchronized (TestHttpServer.class)
      {
         TestHttpServer.class.wait();
      }
      log.info("started server");
   }
   
   
   protected void doTest(Map metadata, String auth) throws Throwable
   {  
      // Create client.
      String locatorURI = "http://" + server.host + ":" + server.port;
      log.info("connecting to " + locatorURI);
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "10000");
      Client client = new Client(clientLocator, config);
      client.connect();
      log.info("client is connected");
      try
      {
         client.invoke("abc", metadata);
      }
      catch (Throwable t)
      {
         log.error("invoke failed", t);
         fail();
      }
      // Verify correct authorization was sent.
      assertEquals(auth, server.auth);
      client.disconnect();
   }
   
   
   static class TestHttpServer extends Thread
   {
      public String host;
      public int port;
      public String auth;
      
      public void run()
      {  
         try
         {
            log.info("starting HTTP server");
            InetAddress localHost = InetAddress.getLocalHost();
            final ServerSocket ss = new ServerSocket(0, 100, localHost);
            host = localHost.getHostAddress();
            port = ss.getLocalPort();
            synchronized (TestHttpServer.class)
            {
               TestHttpServer.class.notify();
            }
            Socket s = ss.accept();
            
            InputStreamReader ir = new InputStreamReader(s.getInputStream());
            char[] cbuf = new char[1024];
            int len = ir.read(cbuf);
            String request = String.copyValueOf(cbuf, 0, len);
            log.info("Request:");
            System.out.println();
            System.out.println(request);
            System.out.println();
            auth = getAuth(request);
            
            log.info("writing response");
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeBytes("HTTP/1.1 200 OK" + "\r\n");
            dos.writeBytes("Server: testServer");
            dos.writeBytes("Content-Type: text/html" + "\r\n");
            dos.writeBytes("Content-Length: 0\r\n");
            dos.writeBytes("Connection: close\r\n");
            dos.writeBytes("\r\n");
            log.info("wrote response");
            
            ir.close();
            dos.close();
            s.close();
            ss.close();
            log.info("closed HTTP server");
         }
         catch (EOFException e1)
         {
            log.info("end of file");
         } 
         catch (Exception e2)
         {
            log.error("error", e2);
         }
      }
      
      private String getAuth(String request)
      {
         String auth = null;
         String[] tokens = request.split("[\r\n]+");
         for (int i = 0; i < tokens.length; i++)
         {
            if (tokens[i].startsWith("Proxy-Authorization"))
            {
               auth = tokens[i].split(":[ ]*")[1];
               break;
            }
         }
         return auth;
      }
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}