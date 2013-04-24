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
package org.jboss.test.remoting.lease.timer;

import junit.framework.TestCase;
import org.jboss.remoting.util.TimerUtil;

import java.util.TimerTask;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class TimerUtilTestCase extends TestCase
{
   private boolean[] fires = new boolean[10];
   private int counter = 0;

   public void testTimerUtit() throws Exception
   {
      TimerTask task = new TestTimerTask();

      TimerUtil.schedule(task, 5000);

      // fire 0
      Thread.currentThread().sleep(6000);

      task.cancel();

      Thread.currentThread().sleep(6000);

      task = new TestTimerTask();
      TimerUtil.schedule(task, 5000);

      // fire 1 & 2
      Thread.currentThread().sleep(12000);

      task.cancel();

      Thread.currentThread().sleep(12000);

      task = new TestTimerTask();
      TimerUtil.schedule(task, 5000);

      // fire 3
      Thread.currentThread().sleep(6000);

      task.cancel();

      // collect results
      int firedCounter = 0;
      for(int x = 0; x < fires.length; x++)
      {
         boolean fired = fires[x];
         if(fired)
         {
            firedCounter++;
         }
      }

      assertEquals(4, firedCounter);
   }

   public class TestTimerTask extends TimerTask
   {

      public void run()
      {
         System.out.println("timer fire: " + counter);
         fires[counter++] = true;
      }
   }
}