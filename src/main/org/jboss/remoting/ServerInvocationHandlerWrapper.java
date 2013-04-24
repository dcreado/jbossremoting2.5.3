
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
package org.jboss.remoting;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.MBeanServer;

import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.util.SecurityUtility;

/**
 * A ServerInvocationHandlerWrapper is used to wrap an MBean proxy that implements
 * org.jboss.remoting.ServerInvocationHandler.  If necessary, each call will go 
 * through an AccessController.doPrivileged() call.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 4, 2008
 * </p>
 */
public class ServerInvocationHandlerWrapper implements ServerInvocationHandler
{
   private ServerInvocationHandler proxy;
   
   public ServerInvocationHandlerWrapper(ServerInvocationHandler proxy)
   {
      this.proxy = proxy;
   }
   
   public void addListener(final InvokerCallbackHandler callbackHandler)
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.addListener(callbackHandler);
         return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.addListener(callbackHandler);
            return null;
         }
      });
   }

   public Object invoke(final InvocationRequest invocation) throws Throwable
   {
      if (SecurityUtility.skipAccessControl())
      {
         return proxy.invoke(invocation);
      }

      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               try
               {
                  return proxy.invoke(invocation);
               }
               catch (Throwable t)
               {
                  throw new Exception(t);
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause.getCause() == null)
            throw cause;
         else
            throw cause.getCause();
      }
   }

   public void removeListener(final InvokerCallbackHandler callbackHandler)
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.removeListener(callbackHandler);
         return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.removeListener(callbackHandler);
            return null;
         }
      });
   }

   public void setInvoker(final ServerInvoker invoker)
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.setInvoker(invoker);
         return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setInvoker(invoker);
            return null;
         }
      });
   }

   public void setMBeanServer(final MBeanServer server)
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.setMBeanServer(server);
         return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setMBeanServer(server);
            return null;
         }
      });
   }
}