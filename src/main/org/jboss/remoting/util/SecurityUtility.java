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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.remoting.Remoting;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 31, 2008
 * </p>
 */
public class SecurityUtility
{
   static boolean skipAccessControl;
   
   static
   {
      try
      {
         skipAccessControl = ((Boolean)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               boolean b1 = System.getSecurityManager() == null;
               boolean b2 = Boolean.getBoolean(Remoting.SKIP_ACCESS_CONTROL);
//               System.out.println("security manager: " + System.getSecurityManager());
               return new Boolean(b1 || b2);
            }
         })).booleanValue();
      }
      catch (PrivilegedActionException e)
      {
         e.getCause().printStackTrace();
      }
   }
   
   
   static public boolean skipAccessControl()
   {
      return skipAccessControl;
   }
}