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

/*
 * Created on Jul 23, 2005
 */
 
package org.jboss.remoting.transport.multiplex.utility;


/**
 * <code>StoppableThread</code> is the abstract parent of several threads used in the Multiplex system.
 * It is distinguished by a <code>shutdown()</code> method that facilitates termination. 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public abstract class StoppableThread extends Thread
{
   protected boolean running = false;
   protected boolean stopped = true;
   protected boolean terminatedOnError = false;
   
/**
 * 
 */
   public void run()
   {
      running = true;
      stopped = false;
      doInit();
      
      while (running)
         doRun();
      
      doShutDown();
      stopped = true;
   }
   
   
/**
 *
 */
   protected abstract void doInit();
   
   
/**
 *
 */
   protected abstract void doRun();
   
   
/**
 *
 */
   protected abstract void doShutDown();
   
   
/**
* 
* @return
*/
   public synchronized boolean isRunning()
   {
      return running;
   }
   
   
/**
* 
* @return
*/
   public synchronized boolean isStopped()
   {
      return stopped;
   }
   
   
/**
*
*/     
   public synchronized void shutdown()
   {
      running = false;
   }
}
