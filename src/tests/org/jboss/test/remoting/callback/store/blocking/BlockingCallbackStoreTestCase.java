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
package org.jboss.test.remoting.callback.store.blocking;

import junit.framework.TestCase;
import org.jboss.remoting.callback.BlockingCallbackStore;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class BlockingCallbackStoreTestCase extends TestCase
{
   private boolean failure = false;
   private static int lowCounter = 0;
   private static int highCounter = 500;


   public void testBlockingStore() throws Exception
   {

      final BlockingCallbackStore store = new BlockingCallbackStore();

      new Thread(new Runnable() {
         public void run()
         {
            for(int x = 0; x < 100; x++)
            {
               try
               {
                  store.add(new Holder(x));
               }
               catch (IOException e)
               {
                  failure = true;
                  e.printStackTrace();
               }
            }
         }
      }).start();

      new Thread(new Runnable() {
         public void run()
         {
            for(int i = 500; i < 600; i++)
            {
               try
               {
                  store.add(new Holder(i));
               }
               catch (IOException e)
               {
                  failure = true;
                  e.printStackTrace();
               }
            }
         }
      }).start();

      Thread.sleep(1000);

      Holder holder = (Holder)store.getNext();
      while(holder != null)
      {
         int num = holder.getNum();
         if(num <= 100)
         {
            System.out.println("lowCounter = " + lowCounter + ", num = " + num);
            assertEquals(lowCounter, num);
            lowCounter++;
         }
         else
         {
            System.out.println("highCounter = " + highCounter + ", num = " + num);
            assertEquals(highCounter, num);
            highCounter++;
         }
         Thread.sleep(100);
         holder = (Holder)store.getNext();
      }

      assertFalse(failure);

   }

   public class Holder implements Serializable
   {
      private int num = 0;

      public Holder(int num)
      {
         this.num = num;
      }
      public int getNum()
      {
         return num;
      }
   }
}