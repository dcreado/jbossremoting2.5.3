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

package org.jboss.test.remoting.transport.socket.ssl.basic;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

import junit.framework.TestCase;


/**
 * This is the actual concrete test for the invoker client.  Uses socket transport by default.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerClientTest extends TestCase implements SSLInvokerConstants
{
   private Client client;
   private static final Logger log = Logger.getLogger(InvokerClientTest.class);

   public void init()
   {
      try
      {
         // since doing basic (using default ssl server socket factory)
         // need to set the system properties to the truststore
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + host + ":" + getPort());
         client = new Client(locator, "mock");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }

   public void testRemoteCall() throws Throwable
   {
      log.debug("running testRemoteCall()");

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testRemoteCall() invocation of foo.", "bar".equals(ret));
      if("bar".equals(ret))
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED");
      }
      assertEquals("bar", ret);

   }
   
   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }

   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

   public void setUp() throws Exception
   {
      init();
   }

   public void tearDown() throws Exception
   {
      if(client != null)
      {
         client.disconnect();
         client = null;
      }
   }

}
