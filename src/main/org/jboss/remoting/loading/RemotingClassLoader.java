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

package org.jboss.remoting.loading;

import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 5868 $
 */
public class RemotingClassLoader extends ClassLoader
{
   private ClassLoader userClassLoader = null;
   private int referenceCounter;
   private boolean parentFirstDelegation;

   protected static final Logger log = Logger.getLogger(RemotingClassLoader.class);
   protected static final boolean isTrace = log.isTraceEnabled();

   public RemotingClassLoader(ClassLoader remotingClassLoader, ClassLoader userClassLoader)
   {
      this(remotingClassLoader, userClassLoader, true);
   }
   public RemotingClassLoader(ClassLoader remotingClassLoader, ClassLoader userClassLoader,
         boolean parentFirstDelegation)
   {
      super(remotingClassLoader);
      this.userClassLoader = userClassLoader;
      this.parentFirstDelegation = parentFirstDelegation;
      referenceCounter = 1;
   }

   /*
    * Note: This method is called only from MicroRemoteClientInvoker.invoke() while a lock
    * on MicroRemoteClientInvoker.class is held.
    */
   public void setUserClassLoader(ClassLoader userClassLoader)
   throws Exception
   {
      if (this.userClassLoader == null)
      {
         this.userClassLoader = userClassLoader;
      }
      else if (this.userClassLoader != userClassLoader)
      {
         throw new Exception("Attempting to change existing userClassLoader");
      }
      referenceCounter++;
   }
   
   /*
    * Note: This method is called only from MicroRemoteClientInvoker.invoke() while a lock
    * on MicroRemoteClientInvoker.class is held.
    */
   public void unsetUserClassLoader()
   {
      if (--referenceCounter == 0)
         userClassLoader = null;
   }

   public Class loadClass(String name) throws ClassNotFoundException
   {
      Class loadedClass = null;

      ClassLoader parent = getParent();
      if (this.parentFirstDelegation || userClassLoader == null)
         loadedClass = loadClassDelegate(name, parent, userClassLoader);
      else
         loadedClass = loadClassDelegate(name, userClassLoader, parent);
         
      if(loadedClass == null)
      {
         loadedClass = ClassLoaderUtility.loadClass(name, getClass());
      }

      return loadedClass;
   }

   /**
    * Try to load the named class using the primary and secondary class loaders.
    * @param name - the class name to load
    * @param primary - the initial class loader to delegate to
    * @param secondary - the backup class loader to delegate to
    * @return the loaded class
    * @throws ClassNotFoundException
    */
   private Class loadClassDelegate(String name, ClassLoader primary, ClassLoader secondary)
      throws ClassNotFoundException
   {
      Class loadedClass = null;
      try
      {
         loadedClass = Class.forName(name, false, primary);
      }
      catch(ClassNotFoundException e)
      {
         if(isTrace)
         {
            log.trace("Could not load class (" + name + ") using primary class loader (" + primary + ")");
         }
         if(secondary != null)
         {
            try
            {
               loadedClass = Class.forName(name, false, secondary);
            }
            catch (ClassNotFoundException e1)
            {
               if(isTrace)
               {
                  log.trace("Could not load class (" + name + ") using secondary class loader (" + secondary + ")");
               }
            }
         }
      }
      return loadedClass;
   }
}