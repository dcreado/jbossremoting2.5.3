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

package org.jboss.test.remoting.transport.http.method;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;

import javax.management.MBeanServer;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MethodInvocationHandler implements ServerInvocationHandler
{
   public static final String PUBLIC = "Public";
   public static final String ALLOW = "Allow";

   public static final String PUBLIC_VALUE = "OPTIONS, POST, GET, HEAD, PUT";
   public static final String ALLOW_VALUE = "OPTIONS, POST, GET, HEAD, PUT";

   public static final Integer PUT_RESPONSE_CODE = new Integer(201);
   public static final Integer GET_RESPONSE_CODE = new Integer(202);
   public static final Integer HEAD_RESPONSE_CODE = new Integer(204);

   public static final String RESPONSE_HTML = "<html><body>foo</body></html>";

   /**
    * called to handle a specific invocation.  Please take care to make sure
    * implementations are thread safe and can, and often will, receive concurrent
    * calls on this method.
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      Map responseHeaders = invocation.getReturnPayload();
      Map headers = invocation.getRequestPayload();
      String methodType = (String) headers.get(HTTPMetadataConstants.METHODTYPE);
      if(methodType != null)
      {
         if(methodType.equals("OPTIONS"))
         {
            responseHeaders.put(PUBLIC, PUBLIC_VALUE);
            responseHeaders.put(ALLOW, ALLOW_VALUE);
         }
         else if(methodType.equals("GET"))
         {
            responseHeaders.put(HTTPMetadataConstants.RESPONSE_CODE,  GET_RESPONSE_CODE);
            return RESPONSE_HTML;
         }
         else if(methodType.equals("PUT"))
         {
            Object path = headers.get(HTTPMetadataConstants.PATH);
            if((!"/this/is/some/path".equals(path)) && (!"/this/is/some/path/".equals(path)))
            {
               throw new RuntimeException("Path within invocation request payload does not equal '/this/is/some/path', instead is " + path);
            }
            Object protocol = headers.get(HTTPMetadataConstants.HTTPVERSION);
            if(!"HTTP/1.1".equals(protocol))
            {
               throw new RuntimeException("Protocol within invocation request payload does not equal 'HTTP/1.1', instead is " + protocol);
            }
            Object contentType = headers.get("content-type");
            if(!"application/octet-stream".equals(contentType))
            {
               throw new RuntimeException("Content type within invocation request payload does not equal 'application/octet-stream', instead is " + contentType);
            }

            Object putPayload = invocation.getParameter();
            System.out.println("Received PUT object: " + putPayload);
            responseHeaders.put(HTTPMetadataConstants.RESPONSE_CODE, PUT_RESPONSE_CODE);
         }
      }
      return null;
   }


   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
      //NOOP
   }

   /**
    * set the invoker that owns this handler
    *
    * @param invoker
    */
   public void setInvoker(ServerInvoker invoker)
   {
      //NOOP
   }

   /**
    * Adds a callback handler that will listen for callbacks from
    * the server invoker handler.
    *
    * @param callbackHandler
    */
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      //NOOP
   }

   /**
    * Removes the callback handler that was listening for callbacks
    * from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      //NOOP
   }
}