/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.transport.servlet.contenttype;

import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * For JBREM-1101 unit tests.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Sep 1, 2009
 * </p>
 */
public class TestInvocationHandlerCR implements ServerInvocationHandler
{
   public static String CONTENT_TYPE = "test/testContentType";
   public static String INVALID_CONTENT_TYPE_CR = "test/x" + '\r' + "y";
   public static String INVALID_CONTENT_TYPE_LF = "test/x" + '\n' + "y";
   public static String REQUEST = "testRequest";
   public static String RESPONSE = "testResponse";
   
   public void addListener(InvokerCallbackHandler callbackHandler) {}
   public Object invoke(final InvocationRequest invocation) throws Throwable
   {
      Map response = invocation.getReturnPayload();
      if (response != null)
      {
         response.put("Content-Type", INVALID_CONTENT_TYPE_CR);
      }
      return RESPONSE;
   }
   public void removeListener(InvokerCallbackHandler callbackHandler) {}
   public void setMBeanServer(MBeanServer server) {}
   public void setInvoker(ServerInvoker invoker) {}
}
