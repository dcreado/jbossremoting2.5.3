/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.lease.http.multiple;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class HTTPLeaseTestClient extends TestCase
{
   // Default locator values
   private static String transport = "http";
   private static String host = "localhost";
   private static int port = 5400;

   private String locatorURI = transport + "://" + host + ":" + port + "/?" + InvokerLocator.CLIENT_LEASE + "=" + "true";
   private String callbackLocatorURI = transport + "://" + host + ":" + (port + 1);

//   public void setUp()
//   {
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
//      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);
//   }

   protected String getLocatorUri()
   {
      return locatorURI;
   }

   public void testMultipleLeases() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(getLocatorUri());
      System.out.println("Calling remoting server with locator uri of: " + getLocatorUri());

      //InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
      //Connector callbackConnector = new Connector(callbackLocator);
      //callbackConnector.create();
      //callbackConnector.start();

      //TestCallbackHandler callbackHandler = new TestCallbackHandler();

      Map metadata = new HashMap();
      metadata.put("clientName", "test1");
      Client remotingClient1 = new Client(locator, metadata);
      remotingClient1.connect();

      //remotingClient1.addListener(callbackHandler, callbackLocator);

      Object ret = remotingClient1.invoke("test1");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      // now create second client
      Map metadata2 = new HashMap();
      metadata2.put("clientName", "test1");
      Client remotingClient2 =new Client(locator, metadata2);
      remotingClient2.connect();
      //remotingClient2.addListener(callbackHandler, callbackLocator);

      ret = remotingClient2.invoke("test2");
      System.out.println("Response was: " + ret);

      ret = remotingClient1.invoke("test1");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      if(remotingClient1 != null)
      {
         //remotingClient1.removeListener(callbackHandler);
         remotingClient1.disconnect();
      }

      System.out.println("remoting client 1 disconnected");

      //Thread.currentThread().sleep(10000);
      Thread.currentThread().sleep(30000);

      ret = remotingClient2.invoke("test2");
      System.out.println("Response was: " + ret);

      if(remotingClient2 != null)
      {
         //remotingClient2.removeListener(callbackHandler);
         remotingClient2.disconnect();
      }

   }

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("callback: " + callback);
      }
   }


}
