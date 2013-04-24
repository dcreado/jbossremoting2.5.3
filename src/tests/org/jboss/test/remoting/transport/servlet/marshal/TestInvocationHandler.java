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

package org.jboss.test.remoting.transport.servlet.marshal;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;


/**
 * For JBREM-1145.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright August 17, 2009
 * </p>
 */
public class TestInvocationHandler implements ServerInvocationHandler
{
   public static String GET_NUMBER_OF_MARSHALLERS = "getNumberOfMarshallers";
   public static String GET_NUMBER_OF_UNMARSHALLERS = "getNumberOfUnmarshallers";  
   public static String GET_NTH_MARSHALLER_TYPE = "getNthMarshallerType";
   public static String GET_NTH_UNMARSHALLER_TYPE = "getNthUnarshallerType";
   public static String RESET = "reset";
   public static String N = "n";
   
   private static Logger log = Logger.getLogger(TestInvocationHandler.class);
   
   static
   {
      MarshalFactory.addMarshaller("test", new TestMarshaller(), new TestUnMarshaller());
   }
   
   public void addListener(InvokerCallbackHandler callbackHandler) {}

   public Object invoke(final InvocationRequest invocation) throws Throwable
   {
      String s = (String) invocation.getParameter();
      log.info("command: " + s);
      if (RESET.equals(s))
      {
         log.info("doing reset");
         TestMarshaller.marshallers.clear();
         TestUnMarshaller.unmarshallers.clear();
         log.info("TestMarshaller.marshallers: " + TestMarshaller.marshallers);
         log.info("TestUnMarshaller.unmarshallers: " + TestUnMarshaller.unmarshallers);
      }
      else if (GET_NUMBER_OF_MARSHALLERS.equals(s))
      {
         return new Integer(TestMarshaller.marshallers.size());
      }
      else if (GET_NUMBER_OF_UNMARSHALLERS.equals(s))
      {
         return new Integer(TestUnMarshaller.unmarshallers.size());
      }
      else if (GET_NTH_MARSHALLER_TYPE.equals(s))
      {
         int n = Integer.valueOf((String)invocation.getRequestPayload().get(N)).intValue();
         return ((TestMarshaller)TestMarshaller.marshallers.get(n)).type;
      }
      else if (GET_NTH_UNMARSHALLER_TYPE.equals(s))
      {
         int n = Integer.valueOf((String)invocation.getRequestPayload().get(N)).intValue();
         return ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(n)).type;
      }
      else if ("abc".equals(s))
      {
         Map responseMap = invocation.getReturnPayload();
         if (responseMap == null)
         {
            responseMap = new HashMap();
            invocation.setReturnPayload(responseMap);
         }
         responseMap.put(HTTPMetadataConstants.CONTENTTYPE, "text/html");
         return invocation.getParameter();
      }
      
      return invocation.getParameter();
   }
   
   public void removeListener(InvokerCallbackHandler callbackHandler) {}
   public void setMBeanServer(MBeanServer server) {}
   public void setInvoker(ServerInvoker invoker) {}
}