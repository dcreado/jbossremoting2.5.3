/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
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

package org.jboss.test.remoting.security;

import java.util.Map;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;

/**
 * Used by InvokerRegistrySecurityTestCase.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Jul 1, 2010
 * </p>
 */
public class InvokerRegistryCaller
{
   public static void registerInvokerFactories(String transport, Class clientFactory, Class serverFactory)
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.registerInvokerFactories(transport, clientFactory, serverFactory);
   }
   
   public static void unregisterInvokerFactories(String transport)
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.unregisterInvokerFactories(transport);
   }
   
   public static void unregisterLocator(InvokerLocator locator)
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.unregisterLocator(locator);
   }
   
   public static void destroyClientInvoker(InvokerLocator locator, Map map)
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.destroyClientInvoker(locator, map);
   }   

   public static void createClientInvoker(InvokerLocator locator, Map map) throws Exception
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.createClientInvoker(locator, map);
   }
   
   public static void createServerInvoker(InvokerLocator locator, Map map) throws Exception
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.createServerInvoker(locator, map);
   }
   
   public static void destroyServerInvoker(ServerInvoker invoker) throws Exception
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.destroyServerInvoker(invoker);
   }
   
   public static void updateServerInvokerLocator(InvokerLocator locator, InvokerLocator newLocator)
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.updateServerInvokerLocator(locator, newLocator);
   }
   
   public static void isSSLSupported(String transport) throws Exception
   {
      new Exception("InvokerRegistryCaller stacktrace").printStackTrace();
      InvokerRegistry.isSSLSupported(transport);
   }
}
