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

package org.jboss.test.remoting.oneway;

import java.rmi.server.UID;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;

import junit.framework.TestCase;

/**
 * This is the actual concrete test for the invoker client to test oneway calls (client and server based).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class OnewayInvokerClientTest extends TestCase
{
   private static final Logger log = Logger.getLogger(OnewayInvokerClientTest.class);

   private String transport = "socket";
   private int port = 8081;

   private String sessionId = new UID().toString();

   private Client client;

   public void init()
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(transport + "://localhost:" + port);
         client = new Client(locator, "test");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
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
      }
   }

   /**
    * Test simple oneway client invocation
    *
    * @throws Throwable
    */
   public void testOnewayServerInvocation() throws Throwable
   {
      log.debug("running testOnewayClientCallback()");

      sessionId = client.getSessionId();

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke
      String param = "bar";
      makeServerOnewayInvocation("saveInvocationParameter", param);
      Thread.currentThread().sleep(4000);
      Object obj = makeInvocation("getLastInvocationParameter", null);

      checkAssertion(param, obj);
   }

   protected void checkAssertion(String param, Object obj)
   {
      assertEquals(param, obj);
   }

   protected void makeServerOnewayInvocation(String method, String param) throws Throwable
   {
      client.invokeOneway(new NameBasedInvocation(method,
                                                  new Object[]{param},
                                                  new String[]{String.class.getName()}),
                          null,
                          false);

   }

   /**
    * Test simple oneway client invocation
    *
    * @throws Throwable
    */
   public void testOnewayClientInvocation() throws Throwable
   {
      log.debug("running testOnewayClientCallback()");

      sessionId = client.getSessionId();

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke
      String param = "bar";
      makeClientOnewayInvocation("saveInvocationParameter", param);
      Thread.currentThread().sleep(1000);
      Object obj = makeInvocation("getLastInvocationParameter", null);

      checkAssertion(param, obj);

   }

   /**
    * Test simple oneway client invocation
    *
    * @throws Throwable
    */
   public void testClientInvocation() throws Throwable
   {
      log.debug("running testInvocation()");

      sessionId = client.getSessionId();

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke
      String param = "bar";
      Object resp = makeClientInvocation("saveInvocationParameter", param);

      Object obj = makeInvocation("getLastInvocationParameter", null);
      Thread.currentThread().sleep(1000);
      checkAssertion(param, obj);

   }

   protected void makeClientOnewayInvocation(String method, String param) throws Throwable
   {
      client.invokeOneway(new NameBasedInvocation(method,
                                                  new Object[]{param},
                                                  new String[]{String.class.getName()}),
                          null,
                          true);

   }

   protected Object makeClientInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }


   protected Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }


}
