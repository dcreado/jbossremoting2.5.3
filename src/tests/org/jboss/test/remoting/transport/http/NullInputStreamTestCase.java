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
package org.jboss.test.remoting.transport.http;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.http.HTTPClientInvoker;
import org.jboss.remoting.transport.http.WebServerError;


/**
 * Unit test for JBREM-1046.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Oct 27, 2008
 * </p>
 */
public class NullInputStreamTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(NullInputStreamTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected Client client;
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
      
      TestUnMarshaller.clear();
   }

   
   public void tearDown()
   {
      if (client != null)
      {
         client.disconnect();
      }
      if (connector != null)
      {
         connector.destroy();
      }
   }
   
   
   /**
    * Tests default behavior with POST method.
    */
   public void testDefaultBehaviorPost() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "POST");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertTrue(TestUnMarshaller.enteredRead);
      assertTrue(TestUnMarshaller.streamIsNull);

      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests default behavior with HEAD method.
    */
   public void testDefaultBehaviorHead() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "HEAD");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertTrue(TestUnMarshaller.enteredRead);
      assertTrue(TestUnMarshaller.streamIsNull);

      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with unmarshallNullStream == true and with POST method.
    */
   public void testUnmarshalNullStreamTruePost() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(HTTPClientInvoker.UNMARSHAL_NULL_STREAM, "true");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "POST");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertTrue(TestUnMarshaller.enteredRead);
      assertTrue(TestUnMarshaller.streamIsNull);

      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with unmarshallNullStream == true and with HEAD method.
    */
   public void testUnmarshalNullStreamTrueHead() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(HTTPClientInvoker.UNMARSHAL_NULL_STREAM, "true");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "HEAD");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertTrue(TestUnMarshaller.enteredRead);
      assertTrue(TestUnMarshaller.streamIsNull);

      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Tests behavior with unmarshallNullStream == false and with POST method.
    */
   public void testUnmarshalNullStreamFalsePost() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(HTTPClientInvoker.UNMARSHAL_NULL_STREAM, "false");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "POST");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertFalse(TestUnMarshaller.enteredRead);

      log.info(getName() + " PASSES");
   }
   
   
   
   /**
    * Tests behavior with unmarshallNullStream == false and with HEAD method.
    */
   public void testUnmarshalNullStreamFalseHead() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      boolean ok = false; 


      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(HTTPClientInvoker.UNMARSHAL_NULL_STREAM, "false");
      HashMap metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      metadata.put("TYPE", "HEAD");
      
      try
      {
         log.info("response: " + makeInvocation(config, metadata));
         fail("expected WebServerError");
      }
      catch (WebServerError e)
      {
         log.info("received expected WebServerError");
         ok = true;
      }
      
      assertTrue(ok);
      assertFalse(TestUnMarshaller.enteredRead);

      log.info(getName() + " PASSES");
   }
   
   
   protected Object makeInvocation(HashMap config, HashMap metadata) throws Throwable
   {
      // Create client.
      locatorURI = "http://" + host + ":" + port;
      locatorURI += "/?unmarshaller=" + TestUnMarshaller.class.getName();
      log.info("connecting to " + locatorURI);
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      Client client = new Client(clientLocator, config);
      client.connect();
      log.info("client is connected");
      
      try
      {
         return client.invoke("abc", metadata);
      }
      catch (CannotConnectException e)
      {
         log.info("exception thrown during invocation: " + e.getMessage());
         return "CannotConnectException: ok";
      }
   }
   

   protected void setupServer() throws Exception
   {
      log.info("setupServer()");
      InetAddress localHost = InetAddress.getLocalHost();
      final ServerSocket ss = new ServerSocket(0, 100, localHost);
      host = localHost.getHostAddress();
      port = ss.getLocalPort();
      
      new Thread()
      {
         public void run()
         {
            try
            {
               Socket s = ss.accept();
               InputStreamReader ir = new InputStreamReader(s.getInputStream());
               char[] cbuf = new char[1024];
               int len = ir.read(cbuf);
               log.info("Request:");
               System.out.println();
               System.out.println(String.copyValueOf(cbuf, 0, len));
               System.out.println();
               DataOutputStream dos = new DataOutputStream(s.getOutputStream());
               dos.writeBytes("HTTP/1.1 500 error" + "\r\n");
               dos.writeBytes("Server: testServer");
               dos.writeBytes("Content-Type: text/html" + "\r\n");
               dos.writeBytes("Content-Length: 0\r\n");
               dos.writeBytes("Connection: close\r\n");
               dos.writeBytes("\r\n");
               
               ir.close();
               dos.close();
               s.close();
               ss.close();
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
      }.start();
      log.info("started server");
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
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
   
   public static class TestUnMarshaller extends HTTPUnMarshaller
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;

      public static boolean enteredRead;
      public static boolean streamIsNull;
      
      public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
      {
         enteredRead = true;
         streamIsNull = (inputStream == null);
         log.info("entered TestUnMarshaller.read()");
         if (inputStream != null)
         {
            return super.read(inputStream, metadata, version);
         }
         else
         {
            return null;
         }
      }
      
      public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
      {
         HTTPUnMarshaller unmarshaller = new TestUnMarshaller();
         unmarshaller.setClassLoader(getClassLoader());
         return unmarshaller;
      }
      
      public static void clear()
      {
         enteredRead = false;
         streamIsNull = false;
      }
   }
}