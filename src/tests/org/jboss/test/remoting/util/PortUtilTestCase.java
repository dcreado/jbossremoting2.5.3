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
package org.jboss.test.remoting.util;

import org.jboss.remoting.transport.PortUtil;

import junit.framework.TestCase;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PortUtilTestCase extends TestCase
{
   private byte[] portArray = null;
   private static SynchronizedInt counter = new SynchronizedInt(0);
   private static SynchronizedInt countToWait = new SynchronizedInt(0);
   private static int errorPort = 0;
   private static boolean error = false;
   private String host = "localhost";
   //private String host = "192.168.1.202";

   public void setUp()
   {
      portArray = new byte[65536];
   }

   public void testFindingFreePorts()
   {
      int maxWaitNumber = 20;
      int numOfThreads = 100;

      for(int x = 0; x < numOfThreads; x++)
      {
         new Thread(new Runnable()
         {
            public void run()
            {
               try
               {
                  counter.increment();
                  byte portFlag = 1;
                  int port = PortUtil.findFreePort(host);
                  System.out.println("port found: " + port);
                  byte portVal = portArray[port];
                  portArray[port] = portFlag;
                  assertEquals("Found port already in use: " + port, 0, portVal);
                  if(portVal == 1)
                  {
                     error = true;
                     errorPort = port;
                  }
               }
               catch(Throwable e)
               {
                  error = true;
                  assertTrue("Error: " + e.getMessage(), false);
               }
            }
         }).start();
      }

      while(counter.get() < numOfThreads && !error)
      {
         countToWait.increment();

         if(countToWait.get() > maxWaitNumber)
         {
            break;
         }

         try
         {
            Thread.currentThread().sleep(1000);
         }
         catch(InterruptedException e)
         {
            e.printStackTrace();
         }
         System.out.println("counter: " + counter);
      }

      if(error)
      {
         assertTrue("Error in getting free port (duplicate port = " + errorPort + ")", false);
      }
      if(countToWait.get() >= maxWaitNumber)
      {
         assertTrue("Error in getting free port.  Never got to error or reached number of counts.", false);
      }


   }

}