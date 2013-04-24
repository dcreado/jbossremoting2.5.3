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
package org.jboss.test.remoting.concurrent.remote;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ConcurrentTestClient extends TestCase
{
   private String locatorUri = "socket://localhost:8777";
   private boolean failure = false;
   private Client client = null;
   private int numOfThreads = 100;
   private int numOfIterations = 100;
   private int[][] results = null;

   public void setUp() throws Exception
   {
      client = new Client(new InvokerLocator(locatorUri));
      client.connect();
      results = new int[numOfThreads][numOfIterations];
   }


   public void testConcurrentInvocations() throws Exception
   {

      for(int x = 0; x < numOfThreads; x++)
      {
         final int num = x;
         new Thread(new Runnable() {
            public void run()
            {
               try
               {
                  runInvocations(num);
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
         }, "" + x).start();
      }

      Thread.sleep(20000);

      assertFalse(failure);

      assertTrue(validateResults());

   }

   private boolean validateResults()
   {
      boolean failed = true;

      for(int z = 0; z < numOfThreads; z++)
      {
         for(int q = 1; q < numOfIterations; q++)
         {
            int a = results[z][q -1];
            int b = results[z][q];
            //System.out.println("a = " + a + ", b = " + b + ((b -1 != a) ? " - FAILED" : ""));
            if(b - 1 != a)
            {
               failed = false;
            }
         }
      }
      return failed;
   }

   private void runInvocations(int num) throws Throwable
   {
      for(int i = 0; i < numOfIterations; i++)
      {
         String param = num + "-" + i;
         Object result = client.invoke(param);
         //System.out.println(Thread.currentThread() + " - " + result);
         assertEquals(param, result);
         String subResult = ((String)result).substring(String.valueOf(num).length() + 1);
         //System.out.println(Thread.currentThread() + " - " + subResult);
         results[num][i] = Integer.parseInt(subResult);
      }
   }

   public void tearDown()
   {
      if(client != null)
      {
         client.disconnect();
      }
   }

}