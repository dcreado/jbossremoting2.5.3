
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
package org.jboss.remoting.callback;

import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.jboss.remoting.SerializableStore;
import org.jboss.remoting.util.SecurityUtility;

/**
 * A CallbackStoreWrapper is used to wrap an MBean proxy that implements
 * org.jboss.remoting.SerializableStore.  If necessary, each call will go 
 * through an AccessController.doPrivileged() call.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 5, 2008
 * </p>
 */
public class CallbackStoreWrapper implements SerializableStore
{
   private SerializableStore proxy;


   public CallbackStoreWrapper(SerializableStore proxy)
   {
      this.proxy = proxy;
   }
   
   public void add(final Serializable object) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.add(object);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               proxy.add(object);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   public void create() throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.create();
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               proxy.create();
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
   }

   public void destroy()
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.destroy();
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.destroy();
            return null;
         }
      });
   }

   public Object getNext() throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return proxy.getNext();
      }
      
      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return proxy.getNext();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   public boolean getPurgeOnShutdown()
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.getPurgeOnShutdown();
      }

      return ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(proxy.getPurgeOnShutdown());
         }
      })).booleanValue();
   }

   public void purgeFiles()
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.purgeFiles();
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.purgeFiles();
            return null;
         }
      });
   }

   public void setConfig(final Map config)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.setConfig(config);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setConfig(config);
            return null;
         }
      });
   }

   public void setPurgeOnShutdown(final boolean purgeOnShutdown)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.setPurgeOnShutdown(purgeOnShutdown);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setPurgeOnShutdown(purgeOnShutdown);
            return null;
         }
      });
   }

   public int size()
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.size();
      }

      return ((Integer) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Integer(proxy.size());
         }
      })).intValue();
   }

   public void start() throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.start();
          return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               proxy.start();
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
   }

   public void stop()
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.stop();
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.stop();
            return null;
         }
      });
   }
}