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
package org.jboss.test.remoting.transport.http.chunked;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ChunkedClient extends TestCase
{
   private String locatorUrl = "http://localhost:8777/?datatype=chunked&" +
                               "marshaller=org.jboss.test.remoting.transport.http.chunked.ChunkedMarshaller&" +
                               "unmarshaller=org.jboss.test.remoting.transport.http.chunked.ChunkedUnMarshaller";

   public void testChunkedViaMap() throws Throwable
   {
      System.gc();

      Map config = new HashMap();
      config.put("chunkedLength", "2048");
      Client client = new Client(new InvokerLocator(locatorUrl), config);
      client.connect();

      Object response = client.invoke("foobar");

      client.disconnect();

      System.out.println("response was " + response);
      assertEquals("barfoo", response);

      System.gc();

   }
}