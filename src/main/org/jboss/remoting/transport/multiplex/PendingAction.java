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
 * Created on Oct 22, 2005
 */

package org.jboss.remoting.transport.multiplex;


/**
 * Each <code>PendingAction</code> holds information about an action that will be performed
 * on the <code>MultiplexingManager.PendingActionThread</code>.  These are time consuming
 * activities that should not be executed on the "main" thread.
 * For more information, see <code>MultiplexingManager</code>. 
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public abstract class PendingAction
{
   protected Object o;
   
   public PendingAction()
   {   
   }
   
   public PendingAction(Object o)
   {
      this.o = o;
   }
   
   abstract void doAction();
}

