
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
package org.jboss.remoting.serialization.impl.java;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.logging.Logger;
import org.jboss.remoting.util.SecurityUtility;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Aug 27, 2008
 * </p>
 */
public class ClearableObjectOutputStream extends ObjectOutputStream
{
   protected static Logger log = Logger.getLogger(ClearableObjectOutputStream.class);
   protected static Method clearMethod;
   protected static Object[] PARAMS = new Object[]{};
   
   static
   {
      try
      {
         clearMethod = getDeclaredMethod(ObjectOutputStream.class, "clear", new Class[]{});
      }
      catch (SecurityException e)
      {
         log.error(e.getMessage(), e);
      }
      catch (NoSuchMethodException e)
      {
         log.error(e.getMessage(), e);
      }
   }
   
   public ClearableObjectOutputStream(OutputStream out) throws IOException
   {
      super(out);  
   }
   
   public void clear()
   {
      try
      {
          clearMethod.invoke(this, PARAMS);
      }
      catch (Throwable e)
      {
          log.error(e.getMessage(), e);
      }
   }
   
   static private Method getDeclaredMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         Method m = c.getDeclaredMethod(name, parameterTypes);
         m.setAccessible(true);
         return m;
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               Method m = c.getDeclaredMethod(name, parameterTypes);
               m.setAccessible(true);
               return m;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
}

