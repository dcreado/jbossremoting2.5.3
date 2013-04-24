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
package org.jboss.test.remoting.callback.push.bidirectional.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class LocalCallbackTestCase extends TestCase
{
   private Connector connector = null;

   private boolean gotCallback = false;
   private String locatorUri = "socket://localhost:8888";

   public void setUp() throws Exception
   {
      connector = new Connector(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();
   }

   public void testCallback() throws Throwable
   {
      Client client = new Client(new InvokerLocator(locatorUri));
      client.connect();
      InvokerCallbackHandler testCallbackHandler = new TestCallbackHandler();
      client.addListener(testCallbackHandler, new HashMap(), "foobar");
      client.invoke("foobar");

      Thread.sleep(5000);

      client.removeListener(testCallbackHandler);
      client.disconnect();

      assertTrue(gotCallback);

   }

   public void tearDown()
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("callback = " + callback);
         Object handle = callback.getCallbackHandleObject();
         if ("foobar".equals(handle))
         {
            gotCallback = true;
         }
      }
   }


   public class TestInvocationHandler implements ServerInvocationHandler
   {

      private List listeners = new ArrayList();

      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         for (int x = 0; x < listeners.size(); x++)
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) listeners.get(x);
            callbackHandler.handleCallback(new Callback("This is callback payload"));
         }
         return "barfoo";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.remove(callbackHandler);
      }
   }

}