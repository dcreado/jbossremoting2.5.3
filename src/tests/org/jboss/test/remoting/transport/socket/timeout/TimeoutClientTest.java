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

package org.jboss.test.remoting.transport.socket.timeout;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutClientTest extends TestCase
{
//   private String locatorURI = "socket://localhost:8899/?timeout=3000";
//   private String locatorURI = "socket://localhost:8899";

   protected String getTransport()
   {
      return "socket";
   }
   
   public void testTimeout() throws Exception
   {
      Map config = new HashMap();
      config.put("timeout", "3000");
      String locatorURI = getTransport() + "://localhost:8899";
      Client client = new Client(new InvokerLocator(locatorURI), config);
      client.connect();

      //test for client timeout
      try
      {
         client.invoke("foo");

         Thread.currentThread().sleep(5000);
         client.invoke("bar");
         System.out.println("Done making all calls after sleeping.");
      }
      catch(Throwable throwable)
      {
         if(throwable instanceof Exception)
         {
            throw (Exception) throwable;
         }
         else
         {
            throw new Exception(throwable);
         }
      }


      long start = System.currentTimeMillis();

      long end = 0;

      try
      {
         client.invoke("timeout");
         end = System.currentTimeMillis();
      }
      catch(Throwable t)
      {
         System.out.println("Caught exception: " + t.getMessage());
         end = System.currentTimeMillis();
      }

      long executionTime = end - start;
      System.out.println("execution time was " + executionTime);
      boolean timedOut = (executionTime < 10000);
      assertTrue("Socket did not timeout within expected time", timedOut);
   }

}