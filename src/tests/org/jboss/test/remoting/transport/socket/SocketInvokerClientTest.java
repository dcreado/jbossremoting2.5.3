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

package org.jboss.test.remoting.transport.socket;

import org.jboss.test.remoting.transport.InvokerClientTest;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketInvokerClientTest extends InvokerClientTest
{
   public String getTransport()
   {
      return "socket";
   }

   public static void main(String[] args)
   {
      SocketInvokerClientTest client = new SocketInvokerClientTest();
      try
      {
         client.setUp();
         client.testArrayReturn();
         client.testLocalPushCallback();
         client.testPullCallback();
         client.testPullCallback();
         client.testRemotePushCallback();
         client.testThrownException();
         client.tearDown();
      }
      catch (Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }
}