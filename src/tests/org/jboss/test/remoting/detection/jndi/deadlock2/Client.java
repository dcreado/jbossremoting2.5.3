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
package org.jboss.test.remoting.detection.jndi.deadlock2;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerLocator;

import java.io.IOException;
import java.net.Socket;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client extends TestCase
{
   private String locatorUri = "socket://localhost:8765";
//   private org.jboss.remoting.Client client = null;

//   public void setUp() throws Exception
//   {
//      client = new org.jboss.remoting.Client(new InvokerLocator(locatorUri));
//      client.connect();
//   }

   public void testDeadlock() throws Throwable
   {
      org.jboss.remoting.Client client = new org.jboss.remoting.Client(new InvokerLocator(locatorUri));
      client.connect();
      Object ret = client.invoke("foobar");

      System.out.println("first response: " + ret);

      try
      {
         Socket socket = new Socket("localhost", 8765);
         socket.close();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      Object ret2 = client.invoke("barfoo");

      System.out.println("second response: " + ret2);

      client.disconnect();

      org.jboss.remoting.Client client2 = new org.jboss.remoting.Client(new InvokerLocator(locatorUri));
      client2.connect();
      Object ret3 = client2.invoke("foo");
      System.out.println("third response: " + ret3);
      client2.disconnect();
   }

//   public void tearDown()
//   {
//      if(client != null)
//      {
//         client.disconnect();
//      }
//   }
}