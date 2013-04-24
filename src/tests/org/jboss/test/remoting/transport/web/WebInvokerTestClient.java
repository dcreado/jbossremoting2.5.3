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

package org.jboss.test.remoting.transport.web;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.http.WebServerError;
import org.jboss.remoting.transport.web.WebUtil;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class WebInvokerTestClient extends TestCase
{
   protected static Logger log = Logger.getLogger(WebInvokerTestClient.class);
   
   public abstract String getLocatorURI();

   public void testPostInvocation() throws Exception
   {
      testPostInvocationSub(true);
      testPostInvocationSub(false);
      checkContentType();
   }
   
   /**
    * If raw == true, set content-type appropriately.
    * If raw == false, all content will be binary and well be treated as binary.
    */
   public void testPostInvocationSub(boolean raw) throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         log.debug("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         
         if (raw)
         {

            // The following use of two versions of Client.RAW is to account for the
            // fact that the value of Client.RAW changed from Remoting 1.4.x to
            // Remoting 2.0.0.  This test is used as part of the version compatibility
            // test suite.  Yuck.
            metadata.put("rawPayload", Boolean.TRUE);
            metadata.put("RAW_PAYLOAD", Boolean.TRUE);
         }
         
         metadata.put("TYPE", "POST");

         Properties headerProps = new Properties();
         
         if (raw)
         {
            headerProps.put(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
         }
         else
         {
            headerProps.put("Content-Type", WebUtil.BINARY);
            headerProps.put(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING);
         }
         
         addHeaders(headerProps);
         metadata.put("HEADER", headerProps);
         
         
         Properties headerProps1 = new Properties(headerProps);
         HashMap onewayMetadata1 = new HashMap(metadata);
         onewayMetadata1.put("HEADER", headerProps1);
         remotingClient.invokeOneway("Do something", onewayMetadata1, true);
 
         Properties headerProps2 = new Properties(headerProps);
         HashMap onewayMetadata2 = new HashMap(metadata);
         onewayMetadata2.put("HEADER", headerProps2);
         remotingClient.invokeOneway("Do something", onewayMetadata2, false);
         Thread.sleep(100);

         // test with null return expected
         Object response = null;
         response = remotingClient.invoke(WebInvocationHandler.NULL_RETURN_PARAM, metadata);
         log.debug("First response should be null and was: " + response);
         assertNull(response);

         response = remotingClient.invoke("Do something", metadata);
         log.debug("Second response should be " + WebInvocationHandler.HTML_PAGE_RESPONSE + " and was: " + response);
         assertEquals(WebInvocationHandler.HTML_PAGE_RESPONSE, response);

         if (raw)
         {
            headerProps.put("Content-Type", WebUtil.BINARY);
         }
         response = remotingClient.invoke(new ComplexObject(2, "foo", true), metadata);
         log.debug("Third response should be " + WebInvocationHandler.OBJECT_RESPONSE_VALUE + " and was: " + response);
         assertEquals(WebInvocationHandler.OBJECT_RESPONSE_VALUE, response);

         response = remotingClient.invoke(new ComplexObject(2, "foo", true, 3000), metadata);
         log.debug("Fourth response should be " + WebInvocationHandler.LARGE_OBJECT_RESPONSE_VALUE + " and was: " + response);
         assertEquals(WebInvocationHandler.LARGE_OBJECT_RESPONSE_VALUE, response);

         if (raw)
         {
            headerProps.put("Content-Type", "application/soap+xml");
         }
         response = remotingClient.invoke(WebInvocationHandler.STRING_RETURN_PARAM, metadata);
         log.debug("Fifth response should be " + WebInvocationHandler.RESPONSE_VALUE + " and was: " + response);
         assertEquals(WebInvocationHandler.RESPONSE_VALUE, response);

         checkUserAgent(remotingClient, metadata);
         
         makeExceptionInvocation(remotingClient, metadata);
         
      }
      catch (Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if (remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }
   }

   protected void checkUserAgent(Client remotingClient, Map metadata)
         throws Throwable
   {
      Object response;
      String remotingUserAgentValue = "JBossRemoting - ";
      response = remotingClient.invoke(WebInvocationHandler.USER_AGENT_PARAM, metadata);
      log.debug("Sixth response start with " + remotingUserAgentValue + " and was: " + response);
      boolean correctUserAgent = ((String) response).startsWith(remotingUserAgentValue);
      assertTrue("User-Agent should be begin with " + remotingUserAgentValue + " but was " + response, correctUserAgent);
   }

   protected void makeExceptionInvocation(Client remotingClient, Map metadata)
         throws Throwable
   {

      Object response = null;

      try
      {
         log.debug("making exception invocation");
         response = remotingClient.invoke(WebInvocationHandler.THROW_EXCEPTION_PARAM, metadata);
         assertTrue("Should have thrown WebServerError and not made it to here.", false);
      }
      catch (Exception error)
      {
         log.debug("exception: " + error + " " + error.getMessage());
         // having to check class name instead of just catching type WebServerError so
         // can use for backwards compatibility tests since WebServerError is new since 2.0.0.CR1.

         if (getLocatorURI().indexOf("dont-return-exception=true") >= 0)
         {
            assertTrue("Did not get WebServerError", error instanceof WebServerError);
            assertNotNull(error.getMessage());
            assertTrue(error.getMessage().indexOf("Error occurred processing invocation request. ") >= 0);
         }
         else if (Boolean.TRUE.equals(metadata.get("rawPayload")) ||
                  Boolean.TRUE.equals(metadata.get("RAW_PAYLOAD")))
         {
            assertTrue("Did not get WebServerError", error instanceof WebServerError);
            assertNotNull(error.getMessage());
            log.debug("message: " + error.getMessage());
            log.debug("message type: " + error.getMessage().getClass());
            assertTrue(error.getMessage().startsWith("Error received when calling on web server."));
         }
         else
         {
            assertTrue("Did not get WebTestException", error instanceof WebTestException);
         }
      }

      metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      response = remotingClient.invoke(WebInvocationHandler.THROW_EXCEPTION_PARAM, metadata);
      if (response instanceof Exception)
      {
         log.debug("Return from invocation is of type Exception as expected.");
         assertTrue("Received exception return as expected.", true);
      }
      else
      {
         log.info("Did not get Exception type returned as expected.");
         assertTrue("Should have received Exception as return.", false);
      }
      metadata.remove(HTTPMetadataConstants.NO_THROW_ON_ERROR);
   }
   
   protected void checkContentType() throws Exception
   {
      log.debug("check_content_type: " + System.getProperty("check_content_type"));
      String s = System.getProperty("check_content_type", "true");
      boolean doCheck = Boolean.valueOf(s).booleanValue();
      if (!doCheck)
      {
         log.debug("skipping content type check");
         return;
      }
      
      String urlString = getLocatorURI();
      int pos = urlString.indexOf("servlet");
      if (pos == 0)
      {
         urlString = "http" + urlString.substring("servlet".length());
      }
      pos = urlString.indexOf("sslservlet");
      if (pos == 0)
      {
         urlString = "https" + urlString.substring("sslservlet".length());
      }
      
      URL url = new URL(urlString);
      OutputStream os = null;
      String contentType = null;
      
      if (urlString.startsWith("https"))
      {
         HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
         conn.setDoOutput(true);
         conn.setDoInput(true);
         conn.addRequestProperty(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
         os = conn.getOutputStream();
         byte[] requestBytes = WebInvocationHandler.SET_CONTENT_TYPE.getBytes();
         os.write(requestBytes);
         contentType = conn.getContentType();
      }
      else
      {
         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
         conn.setDoOutput(true);
         conn.setDoInput(true);
         conn.addRequestProperty(HTTPMetadataConstants.REMOTING_CONTENT_TYPE, HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING);
         os = conn.getOutputStream();
         byte[] requestBytes = WebInvocationHandler.SET_CONTENT_TYPE.getBytes();
         os.write(requestBytes);
         contentType = conn.getContentType();
      }
      
      // Verify that content-type is the value set in the ServerInvocationHandler.
      log.debug("content-type: " + contentType);
      assertEquals(WebInvocationHandler.CONTENT_TYPE, contentType);
   }

   public void testGetInvocation() throws Exception
   {
      testGetInvocationSub(true);
      testGetInvocationSub(false);
   }
   
   protected void testGetInvocationSub(boolean raw) throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         log.debug("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         if (raw)
         {
            metadata.put(Client.RAW, Boolean.TRUE);
         }
         metadata.put("TYPE", "GET");

         Object response = null;

         // test with null return expected
         response = remotingClient.invoke((Object) null, metadata);
         log.debug("Response should be " + WebInvocationHandler.HTML_PAGE_RESPONSE + " and was: " + response);
         assertEquals(WebInvocationHandler.HTML_PAGE_RESPONSE, response);

         response = remotingClient.invoke((Object) null, metadata);
         log.debug("Response should be " + WebInvocationHandler.HTML_PAGE_RESPONSE + " and was: " + response);
         assertEquals(WebInvocationHandler.HTML_PAGE_RESPONSE, response);
      }
      catch (Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if (remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }
   }

   protected void addHeaders(Properties headerProps)
   {
      //NO OP - for overriding by sub-classes.
   }
}