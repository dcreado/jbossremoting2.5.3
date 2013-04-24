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
package org.jboss.test.remoting.versioning.transport.http;

import org.jboss.remoting.Client;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.test.remoting.transport.http.HTTPInvokerTestClient;
import org.jboss.test.remoting.transport.web.WebInvocationHandler;

import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class VersionHTTPInvokerTestClient extends HTTPInvokerTestClient
{
   protected void makeExceptionInvocation(Client remotingClient, Map metadata)
         throws Throwable
   {
      try
      {
         // as of 2.0.0.CR1, would throw exception by default from the client if did not have
         // following metadata property.  Since would not be present in older versions, will only
         // test when response is the exception (in older versions the property set will just be ignored
         metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
         Object response = remotingClient.invoke(WebInvocationHandler.THROW_EXCEPTION_PARAM, metadata);
         if (response instanceof Exception)
         {
            System.out.println("Return from invocation is of type Exception as expected.");
            assertTrue("Received exception return as expected.", true);
         }
         else
         {
            System.out.println("Did not get Exception type returned as expected.");
            assertTrue("Should have received Exception as return.", false);
         }
      }
      catch (Exception e)
      {
         log.info("makeExceptionInvocation() caught exception: " + e.getMessage());
      }
   }


   protected void checkUserAgent(Client remotingClient, Map metadata)
   throws Throwable
   {
      // this is going to be no-op since in 1.4.x version, just returned
      // java version as the agent and not jboss remoting.
   }


}