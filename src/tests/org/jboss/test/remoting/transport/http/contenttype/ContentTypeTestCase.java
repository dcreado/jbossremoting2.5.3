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
package org.jboss.test.remoting.transport.http.contenttype;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.web.WebUtil;


/**
 * Unit test for JBREM-653 and JBREM-1101.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 5417 $
 * <p>
 * Copyright Sep 12, 2007
 * </p>
 */
public class ContentTypeTestCase extends TestCase
{
   public static String CONTENT_TYPE = "test/testContentType";
   public static String INVALID_CONTENT_TYPE_CR = "test/x" + '\r' + "y";
   public static String INVALID_CONTENT_TYPE_LF = "test/x" + '\n' + "y";
   public static String REQUEST = "testRequest";
   public static String RESPONSE = "testResponse";
   
   private static Logger log = Logger.getLogger(ContentTypeTestCase.class);
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
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
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
   
   
/**
 * Verifies that content-type may be set in the ServerInvocationHandler.
 */
   public void testContentType() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(CONTENT_TYPE);
      
      // Send a message and receive a response using an HttpURLConnection.
      URL url = new URL(locatorURI);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setDoInput(true);
      byte[] requestBytes = REQUEST.getBytes();
      OutputStream os = conn.getOutputStream();
      os.write(requestBytes);
      String contentType = conn.getContentType();
      log.info("content-type: " + contentType);
      InputStream is = conn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String response = reader.readLine();
      log.info("response: " + response);
      
      // Verify that content-type is the value set in the ServerInvocationHandler.
      assertEquals(CONTENT_TYPE, contentType);
      assertEquals(RESPONSE, response);
      
      teardownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that content-type with CR supplied by ServerInvocationHandler is discarded.
    */
   public void testInvalidContentTypeServerCR() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      setupServer(INVALID_CONTENT_TYPE_CR);

      // Send a message and receive a response using an HttpURLConnection.
      URL url = new URL(locatorURI);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setDoInput(true);
      byte[] requestBytes = REQUEST.getBytes();
      OutputStream os = conn.getOutputStream();
      os.write(requestBytes);
      String contentType = conn.getContentType();
      log.info("content-type: " + contentType);
      InputStream is = conn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String response = reader.readLine();
      log.info("response: " + response);

      // Verify that content-type is the default value.
      log.info("content-type: " + contentType);
      assertEquals(WebUtil.HTML, contentType);
      assertEquals(RESPONSE, response);

      teardownServer();
      log.info(getName() + " PASSES");
   }


   /**
    * Verifies that content-type with LF supplied by ServerInvocationHandler is discarded.
    */
   public void testInvalidContentTypeServerLF() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      setupServer(INVALID_CONTENT_TYPE_LF);

      // Send a message and receive a response using an HttpURLConnection.
      URL url = new URL(locatorURI);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setDoInput(true);
      byte[] requestBytes = REQUEST.getBytes();
      OutputStream os = conn.getOutputStream();
      os.write(requestBytes);
      String contentType = conn.getContentType();
      log.info("content-type: " + contentType);
      InputStream is = conn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String response = reader.readLine();
      log.info("response: " + response);

      // Verify that content-type is the default value.
      log.info("content-type: " + contentType);
      assertEquals(WebUtil.HTML, contentType);
      assertEquals(RESPONSE, response);

      teardownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "http";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   
   
   protected void setupServer(String contentType) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler(contentType);
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void teardownServer()
   {
      if (connector != null)
      {
         connector.stop();
      }
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      String contentType;
      public TestInvocationHandler(String contentType)
      {
         this.contentType = contentType;
      }
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Map response = invocation.getReturnPayload();
         if (response != null)
         {
            response.put("Content-Type", contentType);
         }
         return RESPONSE;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}