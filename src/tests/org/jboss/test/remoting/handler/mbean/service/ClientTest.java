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

package org.jboss.test.remoting.handler.mbean.service;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ClientTest
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   public void makeInvocation(String locatorURI) throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      Client remotingClient = new Client(locator);
      remotingClient.connect();
      Object response = remotingClient.invoke("Do something");

      System.out.println("Invocation response: " + response);
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      String locatorURI = transport + "://" + host + ":" + port;
      ClientTest clientTest = new ClientTest();
      try
      {
         clientTest.makeInvocation(locatorURI);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }

}
