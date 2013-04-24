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
package org.jboss.remoting.serialization;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class ClassLoaderUtility
{

   /**
    * Tries to load the class from the current thread's context class loader. If
    * not successful, tries to load the class from the current instance.
    *
    * @param classname Desired class.
    * @param clazz     Class object used to obtain a class loader
    *                  if no context class loader is available.
    * @return Class, or null on failure.
    */
   public static Class loadClass(String classname, final Class clazz) throws ClassNotFoundException
   {
      ClassLoader loader;

      try
      {
         loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
         if (loader != null)
         {
            return Class.forName(classname, false, loader);
         }
      }
      catch (Throwable t)
      {
      }

      if (clazz != null)
      {
         try
         {
            loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return clazz.getClassLoader();
               }
            });
            if (loader != null)
            {
               return Class.forName(classname, false, loader);
            }
         }
         catch (Throwable t)
         {
         }
      }

      try
      {
         loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return ClassLoader.getSystemClassLoader();
            }
         });
         if (loader != null)
         {
            return Class.forName(classname, false, loader);
         }
      }
      catch (Throwable t)
      {
      }

      throw new ClassNotFoundException(classname);
   }


   /**
    * Tries to load the class from the passed class' classloader, then current thread's context class loader.
    *
    * @param clazz     Class object used to obtain a class loader
    *                  if no context class loader is available.
    * @param classname Desired class.
    * @return Class, or null on failure.
    */
   public static Class loadClass(final Class clazz, String classname) throws ClassNotFoundException
   {
      ClassLoader loader;

      if (clazz != null)
      {
         try
         {
            loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return clazz.getClassLoader();
               }
            });
            
            if (loader != null)
            {
               return Class.forName(classname, false, loader);
            }
         }
         catch (Throwable t)
         {
         }
      }

      try
      {
         loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
         if (loader != null)
         {
            return Class.forName(classname, false, loader);
         }
      }
      catch (Throwable t)
      {
      }


      try
      {
         loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return ClassLoader.getSystemClassLoader();
            }
         });
         if (loader != null)
         {
            return Class.forName(classname, false, loader);
         }
      }
      catch (Throwable t)
      {
      }

      throw new ClassNotFoundException(classname);
   }


}
