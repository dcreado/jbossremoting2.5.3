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

package org.jboss.test.remoting.transport.http.keep_alive;

import org.jboss.remoting.transport.http.HTTPClientInvoker;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.http.HTTPInvokerConstants;
import org.jboss.test.remoting.transport.web.WebInvokerTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StressHTTPInvokerTestClient extends WebInvokerTestClient implements HTTPInvokerConstants
{
   public String getLocatorURI()
   {
      String bindAddr = System.getProperty("jrunit.bind_addr", host);
      String locatorURI = transport + "://" + bindAddr + ":" + port;
      locatorURI += "/?" + HTTPClientInvoker.NUMBER_OF_CALL_ATTEMPTS + "=3";
      String metadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(metadata != null && metadata.length() > 0)
      {
         locatorURI = locatorURI + "&" + metadata;
      }
      log.debug("connecting to: " + locatorURI);
      return locatorURI;
   }


   public void testPostInvocation() throws Exception
   {
      Thread.sleep(5000);
      
      for(int x = 0; x < 1000; x++)
      {
//         if ((x + 1) % 100 == 0) log.info("loop: " + (x + 1));
         log.info("loop: " + (x + 1));
         super.testPostInvocation();
      }
      
      Thread.sleep(5000);
   }
}