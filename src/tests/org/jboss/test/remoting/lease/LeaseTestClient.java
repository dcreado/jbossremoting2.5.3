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

package org.jboss.test.remoting.lease;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class LeaseTestClient extends TestCase
{
   private Client remotingClient = null;

   protected abstract String getLocatorUri();

   public void testLeaseDisconnect() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(getLocatorUri());
      System.out.println("Calling remoting server with locator uri of: " + getLocatorUri());

      Map metadata = new HashMap();
      metadata.put("clientName", "test1");
      remotingClient = new Client(locator, metadata);
      remotingClient.connect();

      Object ret = remotingClient.invoke("test1");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      ret = remotingClient.invoke("test1");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      if(remotingClient != null)
      {
         remotingClient.disconnect();
      }

   }


   public void testLeaseTimeout() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(getLocatorUri());
      System.out.println("Calling remoting server with locator uri of: " + getLocatorUri());

      Map metadata = new HashMap();
      metadata.put("clientName", "test1");
      remotingClient = new Client(locator, metadata);
      remotingClient.connect();

      Object ret = remotingClient.invoke("test3");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      ret = remotingClient.invoke("test2");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      ret = remotingClient.invoke("test2");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(10000);

      ret = remotingClient.invoke("test2");
      System.out.println("Response was: " + ret);

      Thread.currentThread().sleep(1000);

      if(remotingClient != null)
      {
         remotingClient.disconnect();
      }

   }

   public void testLeaseTimeout2() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(getLocatorUri());
      System.out.println("Calling remoting server with locator uri of: " + getLocatorUri());

      Map metadata = new HashMap();
      metadata.put("clientName", "test3");
      remotingClient = new Client(locator, metadata);
      remotingClient.connect();

      Thread.currentThread().sleep(10000);

      remotingClient.disconnect();
   }

}