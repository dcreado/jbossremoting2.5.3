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

package org.jboss.test.remoting.transport.http.raw;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.http.HTTPInvokerConstants;
import org.jboss.test.remoting.transport.web.WebInvocationHandler;


/**
 * This tests the ability of the http transport to correctly handle raw payloads
 * as well as InvocationRequests and InvocationResponses.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 3641 $
 * <p>
 * Copyright Oct 29, 2006
 * </p>
 */
public class HTTPInvokerTestClient extends TestCase implements HTTPInvokerConstants
{
   
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
         
         Object response = remotingClient.invoke("Do something", metadata);
         System.out.println("Second response should be " + WebInvocationHandler.HTML_PAGE_RESPONSE + " and was: " + response);
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
   
   public void testCookedInvocation() throws Exception
   {
      Client remotingClient = null;

      try
      {
         InvokerLocator locator = new InvokerLocator(getLocatorURI());
         System.out.println("Calling remoting server with locator uri of: " + getLocatorURI());

         remotingClient = new Client(locator);
         remotingClient.connect();

         Object response = remotingClient.invoke("Do something");
         System.out.println("Second response should be " + WebInvocationHandler.HTML_PAGE_RESPONSE + " and was: " + response);
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
   
   protected void addHeaders(Properties headerProps)
   {
      //NO OP - for overriding by sub-classes.
   }
}