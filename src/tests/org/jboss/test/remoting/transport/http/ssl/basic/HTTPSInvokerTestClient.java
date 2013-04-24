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

package org.jboss.test.remoting.transport.http.ssl.basic;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.test.remoting.transport.http.HTTPInvokerTestServer;
import org.jboss.test.remoting.transport.http.ssl.SSLInvokerConstants;
import org.jboss.test.remoting.transport.web.ComplexObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSInvokerTestClient extends TestCase implements SSLInvokerConstants
{
   public void testInvocationWithHeaders() throws Exception
   {
      Client remotingClient = null;

      try
      {
         // since doing basic (using default ssl server socket factory)
         // need to set the system properties to the truststore
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

         String locatorURI = transport + "://" + host + ":" + port;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling remoting server with locator uri of: " + locatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put(Client.RAW, Boolean.TRUE);
         metadata.put("TYPE", "POST");

         Properties headerProps = new Properties();
         headerProps.put("SOAPAction", "http://www.example.com/fibonacci");
         headerProps.put("Content-type", "application/soap+xml");

         metadata.put("HEADER", headerProps);

         // set this property so does not do host verification
         metadata.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");

         Object response = null;

         // test with null return expected
         response = remotingClient.invoke(HTTPInvokerTestServer.NULL_RETURN_PARAM, metadata);

         assertNull(response);

         response = remotingClient.invoke("Do something", metadata);

         assertEquals(HTTPInvokerTestServer.RESPONSE_VALUE, response);

         // test with small object
         headerProps.put("Content-type", WebUtil.BINARY);
         response = remotingClient.invoke(new ComplexObject(2, "foo", true), metadata);

         assertEquals(HTTPSInvokerTestServer.OBJECT_RESPONSE_VALUE, response);

         // test with large object
         response = remotingClient.invoke(new ComplexObject(2, "foo", true, 8000), metadata);

         assertEquals(HTTPSInvokerTestServer.LARGE_OBJECT_RESPONSE_VALUE, response);

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


}