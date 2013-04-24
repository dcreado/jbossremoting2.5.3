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

package org.jboss.test.remoting.transport.http.method;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.http.HTTPInvokerConstants;
import org.jboss.test.remoting.transport.web.ComplexObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPInvokerTestClient extends TestCase implements HTTPInvokerConstants
{
   public String getLocatorURI()
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      String locatorURI = transport + "://" + bindAddr + ":" + port;
      String metadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(metadata != null && metadata.length() > 0)
      {
         locatorURI = locatorURI + "/?" + metadata;
      }
      return locatorURI;
   }

   public String getLocatorURIWithPath()
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      String locatorURI = transport + "://" + bindAddr + ":" + port + "/this/is/some/path";
      String metadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(metadata != null && metadata.length() > 0)
      {
         locatorURI = locatorURI + "/?" + metadata;
      }
      return locatorURI;
   }

   public void testOptionsInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put("TYPE", "OPTIONS");

         // test with null return expected
         Object response = remotingClient.invoke((Object) null, metadata);

         assertNull("OPTIONS http invocation should return null", response);
         String publicValue = (String) ((List) metadata.get(MethodInvocationHandler.PUBLIC)).get(0);
         assertEquals("Metadata value for " + MethodInvocationHandler.PUBLIC + " should be " + MethodInvocationHandler.PUBLIC_VALUE +
                      " and was " + publicValue, MethodInvocationHandler.PUBLIC_VALUE, publicValue);
         String allowValue = (String) ((List) metadata.get(MethodInvocationHandler.ALLOW)).get(0);
         assertEquals("Metadata value for " + MethodInvocationHandler.ALLOW + " should be " + MethodInvocationHandler.ALLOW_VALUE +
                      " and was " + allowValue, MethodInvocationHandler.ALLOW_VALUE, allowValue);
      }
      catch(Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }


   }

   public void testPutInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURIWithPath());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put("TYPE", "PUT");

         // test with null return expected
         Object response = remotingClient.invoke(new ComplexObject(2, "foo", true), metadata);
         System.out.println("response: " + response);
         if(response instanceof Exception)
         {
            ((Exception)response).printStackTrace();
         }

         Integer responseCode = (Integer) metadata.get(HTTPMetadataConstants.RESPONSE_CODE);
         assertEquals("Metadata value for " + HTTPMetadataConstants.RESPONSE_CODE + " should be " + MethodInvocationHandler.PUT_RESPONSE_CODE +
                      " and was " + responseCode, MethodInvocationHandler.PUT_RESPONSE_CODE, responseCode);

         Object respHdr = metadata.get(MethodInvocationHandler.PUBLIC);
         assertNull(respHdr);

      }
      catch(Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }


   }

   public void testGetInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURIWithPath());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put("TYPE", "GET");

         // test with null return expected
         Object response = remotingClient.invoke((Object)null, metadata);

         Integer responseCode = (Integer) metadata.get(HTTPMetadataConstants.RESPONSE_CODE);
         assertEquals("Metadata value for " + HTTPMetadataConstants.RESPONSE_CODE + " should be " + MethodInvocationHandler.GET_RESPONSE_CODE +
                      " and was " + responseCode, MethodInvocationHandler.GET_RESPONSE_CODE, responseCode);
         assertEquals("Response value should be " + MethodInvocationHandler.RESPONSE_HTML + " and was " + response,
                      MethodInvocationHandler.RESPONSE_HTML, response);

         Object respHdr = metadata.get(MethodInvocationHandler.PUBLIC);
         assertNull(respHdr);


      }
      catch(Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }


   }

   public void testHeadInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURIWithPath());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put("TYPE", "HEAD");

         // test with null return expected
         Object response = remotingClient.invoke((Object)null, metadata);

         Integer responseCode = (Integer) metadata.get(HTTPMetadataConstants.RESPONSE_CODE);
         assertEquals("Metadata value for " + HTTPMetadataConstants.RESPONSE_CODE + " should be " + MethodInvocationHandler.HEAD_RESPONSE_CODE +
                      " and was " + responseCode, MethodInvocationHandler.HEAD_RESPONSE_CODE, responseCode);


      }
      catch(Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }


   }

   public static void main(String[] args)
   {
      HTTPInvokerTestClient client = new HTTPInvokerTestClient();
      try
      {
         client.testOptionsInvocation();
         client.testPutInvocation();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

}