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

package org.jboss.test.remoting.transport.http.errors;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.WebServerError;
import org.jboss.test.remoting.transport.web.WebInvocationHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ErrorHTTPInvokerTestClient extends TestCase
{
   // Default locator values
   private String transport = "http";
   private String host = "localhost";
   private int port = 8888;

   public String getLocatorURI()
   {
      return transport + "://" + host + ":" + port;
   }


   /**
    * In this case, the payload will be sent raw, the Exception will be return raw,
    * and HTTPClientInvoker should return a WebServerError.
    */
   public void testRawInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put(Client.RAW, Boolean.TRUE);
         metadata.put("TYPE", "POST");

         Properties headerProps = new Properties();
         headerProps.put("Content-type", "application/soap+xml");

         metadata.put("HEADER", headerProps);

         try
         {
            Object response = remotingClient.invoke(WebInvocationHandler.THROW_EXCEPTION_PARAM, metadata);
            assertTrue("Did not get exception thrown as expected.", false);
         }
         catch(WebServerError wse)
         {
            assertTrue("Caught exception as expected", true);
         }
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

   
   /**
    * In this case, the payload will be wrapped in an InvocationRequest, the Exception
    * will be wrapped in an InvocationRequest, and the original Exception should be
    * thrown.
    */
   public void testCookedInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put("TYPE", "POST");

         try
         {
            Object response = remotingClient.invoke(WebInvocationHandler.THROW_EXCEPTION_PARAM, metadata);
            assertTrue("Did not get exception thrown as expected.", false);
         }
         catch(Exception e)
         {
            assertTrue("Caught exception as expected",
                        ErrorHTTPInvokerTestServer.EXCEPTION_MSG.equals(e.getMessage()));
         }
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
      ErrorHTTPInvokerTestClient client = new ErrorHTTPInvokerTestClient();
      try
      {
         client.testRawInvocation();
         client.testCookedInvocation();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}