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

package org.jboss.test.remoting.transporter;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transporter.TransporterClient;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestClient extends TestCase
{
   public void testClientCall() throws Exception
   {
      String locatorURI = "socket://localhost:5400";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      TestServer test = (TestServer) TransporterClient.createTransporterClient(locator, TestServer.class);
      Object response = test.processTestMessage("Hello");
      System.out.println("response is: " + response);
      assertEquals("response should be 'Hello - has been processed'", "Hello - has been processed", response);

      int intresp = test.processInt(123);
      System.out.println("int response is: " + intresp);
      assertEquals("response should be '456'", 456, intresp);

      // now make call that should throw exception
      try
      {
         test.throwException();
         assertTrue("Should have received IOException thrown by server.", false);
      }
      catch(IOException ioex)
      {
         assertTrue("Should have received IOException thrown by server.", true);
      }

      TransporterClient.destroyTransporterClient(test);
   }

   public static void main(String[] args)
   {
      try
      {
         String locatorURI = "socket://localhost:5400";
         InvokerLocator locator = new InvokerLocator(locatorURI);
         TestServer test = (TestServer) TransporterClient.createTransporterClient(locator, TestServer.class);
         Object response = test.processTestMessage("Hello");
         System.out.println("response is: " + response);
         TransporterClient.destroyTransporterClient(test);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}