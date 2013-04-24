package org.jboss.remoting.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;


/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimerUtil
{
   private static Timer timer = null;
   private static ArrayList stoppableTasks = new ArrayList();
   private static Logger log = Logger.getLogger(TimerUtil.class);
   
   private static synchronized void init()
   {
      TimerUtil.timer = new Timer(true);
   }

   public static synchronized void schedule(TimerTask task, long period)
   {
      if (TimerUtil.timer == null)
      {
         TimerUtil.init();
      }
      
      if (task instanceof StoppableTimerTask)
      {
         stoppableTasks.add(task);
      }

      //schedule at fixed delay (not rate)
      try
      {
         TimerUtil.timer.schedule(task, period, period);
      }
      catch (IllegalStateException e)
      {
         log.debug("Unable to schedule TimerTask on existing Timer", e);
         timer = new Timer(true);
         timer.schedule(task, period, period);
      }
   }

   public static synchronized void unschedule(TimerTask task)
   {
      if (!(task instanceof StoppableTimerTask))
      {
         log.warn("TimerUtil only remembers StoppableTimerTasks");
         return;
      }
      
      StoppableTimerTask stoppableTask = (StoppableTimerTask) task;
      if (!stoppableTasks.remove(stoppableTask))
         log.warn("unrecognized StoppableTimerTask: " + task);
      
      try
      {
         stoppableTask.stop();
      }
      catch (Exception e)
      {
         log.warn("error calling stop() on: " + stoppableTask, e);
      }
   }

   public static synchronized void destroy()
   {
      if (TimerUtil.timer != null)
      {
         TimerUtil.timer.cancel();
         TimerUtil.timer = null;
      }
      
      Iterator it = new ArrayList(stoppableTasks).iterator();
      while (it.hasNext())
      {
         StoppableTimerTask task = (StoppableTimerTask) it.next();
         try
         {
            task.stop();
         }
         catch (Exception e)
         {
            log.warn("unable to stop TimerTask: " + task);
         }
      }
   }
}
