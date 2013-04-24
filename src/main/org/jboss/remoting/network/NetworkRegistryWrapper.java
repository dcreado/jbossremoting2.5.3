
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
package org.jboss.remoting.network;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.util.SecurityUtility;


/**
 * A NetworkRegistryWrapper is used to wrap an MBean proxy that implements
 * org.jboss.remoting.network.NetworkRegistryWrapper.  If necessary, each call
 * will go  through an AccessController.doPrivileged() call.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 6, 2008
 * </p>
 */
public class NetworkRegistryWrapper implements NetworkRegistryMBean
{
   private NetworkRegistryMBean proxy;
   
   public NetworkRegistryWrapper(NetworkRegistryMBean proxy)
   {
      this.proxy = proxy;
   }
   
   public void addServer(final Identity identity, final ServerInvokerMetadata[] invokers)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.addServer(identity, invokers);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.addServer(identity, invokers);
            return null;
         }
      });
   }

   public void changeDomain(final String newDomain)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.changeDomain(newDomain);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.changeDomain(newDomain);
            return null;
         }
      });
   }
   
   public NetworkInstance[] getServers()
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.getServers();
      }

      return (NetworkInstance[]) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return proxy.getServers();
         }
      });
   }

   public boolean hasServer(final Identity identity)
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.hasServer(identity);
      }

      return ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(proxy.hasServer(identity));
         }
      })).booleanValue();
   }

   public NetworkInstance[] queryServers(final NetworkFilter filter)
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.queryServers(filter);
      }

      return (NetworkInstance[]) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return proxy.queryServers(filter);
         }
      });
   }

   public void removeServer(final Identity identity)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.removeServer(identity);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.removeServer(identity);
            return null;
         }
      });
   }

   public void updateServer(final Identity identity, final ServerInvokerMetadata[] invokers)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.updateServer(identity, invokers);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.updateServer(identity, invokers);
            return null;
         }
      });
   }
   
   public void addNotificationListener(final NotificationListener listener,
                                       final NotificationFilter filter,
                                       final Object handback)
   throws IllegalArgumentException
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.addNotificationListener(listener, filter, handback);
          return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IllegalArgumentException
            {
               proxy.addNotificationListener(listener, filter, handback);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IllegalArgumentException) e.getCause();
      }
   }

   public MBeanNotificationInfo[] getNotificationInfo()
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.getNotificationInfo();
      }

      return (MBeanNotificationInfo[]) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return proxy.getNotificationInfo();
         }
      });
   }

   public void removeNotificationListener(final NotificationListener listener)
   throws ListenerNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.removeNotificationListener(listener);
          return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws ListenerNotFoundException
            {
               proxy.removeNotificationListener(listener);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (ListenerNotFoundException) e.getCause();
      }
   }
   
   public void postDeregister()
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.postDeregister();
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.postDeregister();
            return null;
         }
      });
   }

   public void postRegister(final Boolean registrationDone)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.postRegister(registrationDone);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.postRegister(registrationDone);
            return null;
         }
      });
   }

   public void preDeregister() throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.preDeregister();
          return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               proxy.preDeregister();
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
   }

   public ObjectName preRegister(final MBeanServer server, final ObjectName name)
   throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
          return proxy.preRegister(server, name);
      }

      try
      {
         return (ObjectName) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return proxy.preRegister(server, name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
   }
}