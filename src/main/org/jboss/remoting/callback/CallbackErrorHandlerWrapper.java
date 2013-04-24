
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.util.SecurityUtility;

/**
 * A CallbackErrorHandlerWrapper is used to wrap an MBean proxy that implements
 * org.jboss.remoting.callback.CallbackErrorHandler.  If necessary, each call
 * will go through an AccessController.doPrivileged() call.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 5, 2008
 * </p>
 */
public class CallbackErrorHandlerWrapper implements CallbackErrorHandler
{
   private CallbackErrorHandler proxy;
   
   public CallbackErrorHandlerWrapper(CallbackErrorHandler proxy)
   {
      this.proxy = proxy;
   }
   
   public void handleError(final Throwable ex) throws Throwable
   {
      if (SecurityUtility.skipAccessControl())
      {
         proxy.handleError(ex);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               try
               {
                  proxy.handleError(ex);
                  return null;
               }
               catch (Throwable e)
               {
                  throw new Exception(ex);
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause.getCause() == null)
            throw cause;
         throw cause.getCause();
      }
   }

   public void setCallbackHandler(final ServerInvokerCallbackHandler serverInvokerCallbackHandler)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.setCallbackHandler(serverInvokerCallbackHandler);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setCallbackHandler(serverInvokerCallbackHandler);
            return null;
         }
      });
   }

   public void setConfig(final Map errorHandlerConfig)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.setConfig(errorHandlerConfig);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setConfig(errorHandlerConfig);
            return null;
         }
      });
   }

   public void setServerInvoker(final ServerInvoker owner)
   {
      if (SecurityUtility.skipAccessControl())
      {
          proxy.setServerInvoker(owner);
          return;
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            proxy.setServerInvoker(owner);
            return null;
         }
      });
   }

}

