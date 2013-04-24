/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.test.remoting.transport.servlet.callback;

import java.util.HashSet;
import java.util.Iterator;

import javax.management.MBeanServer;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;

public class TestInvocationHandler implements ServerInvocationHandler
{
   private static Logger log = Logger.getLogger(TestInvocationHandler.class);
   private HashSet listeners = new HashSet();
   
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      listeners.add(callbackHandler);
      log.info("added "  + callbackHandler);
   }

   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      log.debug("invocation: " + invocation.getParameter());
      Iterator it = listeners.iterator();
      Callback callback = new Callback("callback");
      while (it.hasNext())
      {
         InvokerCallbackHandler handler = (InvokerCallbackHandler) it.next();
         handler.handleCallback(callback);
         log.debug("sent callback to " + handler);
      }
      return null;
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {    
      listeners.remove(callbackHandler);
      log.info("removed " + callbackHandler);
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public void setMBeanServer(MBeanServer server)
   {
   }
}

