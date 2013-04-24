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

package org.jboss.test.remoting.oneway;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;

/**
 * MockServerInvocationHandler
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 566 $
 */
public class OnewayServerInvocationHandler implements ServerInvocationHandler
{
   private ServerInvoker invoker;
   private List listeners = new ArrayList();
   private static final Logger log = Logger.getLogger(OnewayServerInvocationHandler.class);

   private Object lastParam = null;

   /**
    * set the invoker that owns this handler
    *
    * @param invoker
    */
   public void setInvoker(ServerInvoker invoker)
   {
      this.invoker = invoker;
   }

   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
   }

   public Object invoke(InvocationRequest invocation)
         throws Throwable
   {
      Object param = invocation.getParameter();
      String methodName = "";
      Object[] params = null;
      String[] sig = null;

      if(param instanceof RemoteInvocation)
      {
         RemoteInvocation rminvo = (RemoteInvocation) param;
         methodName = rminvo.getMethodName();
         params = rminvo.getParameters();
      }
      else
      {
         throw new Exception("Unknown invocation payload (" + param + ").  " +
                             "Should be instance of RemoteInvocation.");
      }

      Object ret = null;
      if(methodName.equals("saveInvocationParameter"))
      {
         if(params != null)
         {
            lastParam = params[0];
            ret = lastParam;
         }
      }
      else if(methodName.equals("getLastInvocationParameter"))
      {
         ret = lastParam;
      }
      else
      {
         log.error("Expected parameter to be either 'saveInvocationParameter' or 'getLastInvocationParameter'.");
      }
      return ret;
   }


   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      listeners.add(callbackHandler);
      log.debug("added listener " + callbackHandler);
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      listeners.remove(callbackHandler);
      log.debug("removed listener " + callbackHandler);
   }

}
