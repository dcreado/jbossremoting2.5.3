/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.remoting.transport.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.jboss.logging.Logger;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright April 22, 2009
 * </p>
 */
public class TimedOutputStream extends OutputStream
{
   static private Timer timer = new Timer(true);
   static private Logger log = Logger.getLogger(TimedOutputStream.class);
   
   private OutputStream os;
   private int outputTimeout;
   private OutputTimerTask timerTask;
   private Object lock = new Object();
   
   public TimedOutputStream(OutputStream os, int outputTimeout)
   {
      this.os = os;
      this.outputTimeout = outputTimeout;
   }
   
   public void close() throws IOException
   {
      os.close();
   }
   
   public void write(int b) throws IOException
   {
      synchronized (lock)
      {
         if (timerTask == null)
         {
            try
            {
               timerTask = new OutputTimerTask(this);
               timer.schedule(timerTask, outputTimeout);
               if (log.isTraceEnabled()) log.trace("scheduled OutputTimerTask: " + outputTimeout);
            }
            catch (IllegalStateException e)
            {
               timer = new Timer(true);
               timer.schedule(new OutputTimerTask(this), outputTimeout);
               if (log.isTraceEnabled()) log.trace("scheduled OutputTimerTask: " + outputTimeout);
            }
         }
      }
      
      try
      {
         os.write(b);
      }
      finally
      {
         synchronized (lock)
         {
            timerTask.cancel();
            timerTask = null;
         }
      }
   }
   
   public void write(byte b[], int off, int len) throws IOException
   {
      synchronized (lock)
      {
         if (timerTask == null)
         {
            try
            {
               timerTask = new OutputTimerTask(this);
               timer.schedule(timerTask, outputTimeout);
               if (log.isTraceEnabled()) log.trace(this + " scheduled " + timerTask + ": " + outputTimeout);
            }
            catch (IllegalStateException e)
            {
//               timer = new Timer("TimedOutputStreamTimer", true);
               timer = new Timer(true);
               timer.schedule(new OutputTimerTask(this), outputTimeout);
               if (log.isTraceEnabled()) log.trace(this + " scheduled " + timerTask + ": " + outputTimeout);
            }
         }
      }
      
      try
      {
         os.write(b, off, len);
      }
      finally
      {
         synchronized (lock)
         {
            timerTask.cancel();
            timerTask = null;
         }
      }
   }
   
   static class OutputTimerTask extends TimerTask
   {
      private TimedOutputStream tos;
      
      public OutputTimerTask(TimedOutputStream tos)
      {
         this.tos = tos;
      }
      
      public void run()
      {
         try
         {
            log.debug(this + " closing: " + tos);
            tos.close();
            tos = null;
         }
         catch (IOException e)
         {
            log.debug("unable to close " + tos);
         }
      }
      
      public boolean cancel()
      {
         tos = null;
         return super.cancel();
      }
   }
}
