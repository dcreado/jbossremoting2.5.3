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
package org.jboss.remoting.util;


/** 
 * When a StoppableTimerTask is passed to TimerUtil.schedule(), TimerUtil will keep
 * track of it so that it can call StoppableTimerTask.stop() during TimerUtil.destroy().
 * 
 * Note that when a StoppableTimerTask is done, it should call TimerTask.unschedule(this)
 * so that the reference to it is removed.
 * 
 * Note that there is a danger of an infinite loop because if StoppableTimerTask.stop()
 * calls TimerTask.unschedule(this), the TimerTask.unschedule(this) will call 
 * this.StoppableTimerTask().  It follows that StoppableTimerTask.stop() should guard
 * against this danger.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1913 $
 * <p>
 * Copyright Jan 18, 2007
 * </p>
 */
public interface StoppableTimerTask
{
   /**
    * stop() will be called when TimerUtil.unschedule() or when TimerUtil.destroy() 
    * are called.
    * 
    * @throws Exception
    */
   void stop() throws Exception;
}
