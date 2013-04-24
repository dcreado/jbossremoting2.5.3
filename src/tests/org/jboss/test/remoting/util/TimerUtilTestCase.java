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

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.jboss.remoting.util.StoppableTimerTask;
import org.jboss.remoting.util.TimerUtil;

import junit.framework.TestCase;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3391 $
 * <p>
 * Copyright Jan 18, 2007
 * </p>
 */
public class TimerUtilTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(TimerUtilTestCase.class);

   
   public void testTimerTaskDestroy() throws Exception
   {
      log.info("entering " + getName());
      TestTimerTask task = new TestTimerTask();
      TimerUtil.schedule(task, 500);
      Thread.sleep(1000);
      assertTrue(task.ran);
      assertFalse(task.stopped);
      assertFalse(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      
      TimerUtil.destroy();
      
      Thread.sleep(2000);
      assertTrue(task.stopped);
      assertTrue(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      log.info(getName() + " PASSED");
   }
   
   
   public void testTimerTaskUnschedule() throws Exception
   {
      log.info("entering " + getName());
      TestTimerTask task = new TestTimerTask();
      TimerUtil.schedule(task, 500);
      Thread.sleep(1000);
      assertTrue(task.ran);
      assertFalse(task.stopped);
      assertFalse(task.stoppedTwice);
      
      TimerUtil.unschedule(task);
      
      Thread.sleep(2000);
      assertTrue(task.stopped);
      assertTrue(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      log.info(getName() + " PASSED");  
   }
   
   
   public void testTimerTaskStop() throws Exception
   {
      log.info("entering " + getName());
      TestTimerTask task = new TestTimerTask();
      TimerUtil.schedule(task, 500);
      Thread.sleep(1000);
      assertTrue(task.ran);
      assertFalse(task.stopped);
      assertFalse(task.stoppedTwice);
      
      task.stop();
      
      Thread.sleep(2000);
      assertTrue(task.stopped);
      assertTrue(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      log.info(getName() + " PASSED");  
   }
   
   
   /**
    * Unit test for JBREM-851.
    */
   public void testReplaceTimer() throws Exception
   {
      log.info("entering " + getName());
      TestTimerTask task = new TestTimerTask();
      TimerUtil.schedule(task, 500);
      Thread.sleep(1000);
      assertTrue(task.ran);
      assertFalse(task.stopped);
      assertFalse(task.stoppedTwice);
      task.stop();
      Thread.sleep(2000);
      assertTrue(task.stopped);
      assertTrue(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      
      // Give TimerUtil.timer a chance to shut itself down.
      // Note that there is no guarantee that the Timer will, in fact, shut itself
      // down.  The behavior is implementation dependent.
      Thread.sleep(120000);
      task = new TestTimerTask();
      TimerUtil.schedule(task, 500);
      Thread.sleep(1000);
      assertTrue(task.ran);
      assertFalse(task.stopped);
      assertFalse(task.stoppedTwice);
      task.stop();
      assertTrue(task.stopped);
      assertTrue(task.stoppedTwice);
      assertFalse(task.continuedToRun);
      
      log.info(getName() + " PASSED");  
   }
   
   
   class TestTimerTask extends TimerTask implements StoppableTimerTask
   {
      public boolean ran;
      public boolean stopped;
      public boolean stoppedTwice;
      public boolean continuedToRun1;
      public boolean continuedToRun;
      
      public void run()
      {
         log.info("run called");
         ran = true;
         if (continuedToRun1) continuedToRun = true;
         if (stopped) continuedToRun1 = true;
      }

      public void stop() throws Exception
      {
         log.info("stop called");
         if (stopped)
            stoppedTwice = true;
         else
         {
            stopped = true;
            TimerUtil.unschedule(this);
            cancel();
         }
      }
      
   }
}
