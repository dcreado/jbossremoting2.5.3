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

package org.jboss.remoting.samples.http;

import java.util.Map;
import javax.management.MBeanServer;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * Remoting handler implementation for http invocations to
 * show how to return different data to client.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class WebInvocationHandler implements ServerInvocationHandler
{
   // Pre-defined returns to be sent back to client based on type of request.
   public static final String RESPONSE_VALUE = "This is the return to simple text based http invocation.";
   public static final ComplexObject OBJECT_RESPONSE_VALUE = new ComplexObject(5, "dub", false);
   public static final String HTML_PAGE_RESPONSE = "<html><head><title>Test HTML page</title></head><body>" +
                                                   "<h1>HTTP/Servlet Test HTML page</h1><p>This is a simple page served for test." +
                                                   "<p>Should show up in browser or via invoker client</body></html>";

   // Different request types that client may make
   public static final String NULL_RETURN_PARAM = "return_null";
   public static final String OBJECT_RETURN_PARAM = "return_object";
   public static final String STRING_RETURN_PARAM = "return_string";


   /**
    * called to handle a specific invocation
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      // Print out the invocation request
      System.out.println("Invocation request from client is: " + invocation.getParameter());
      if(NULL_RETURN_PARAM.equals(invocation.getParameter()))
      {
         return null;
      }
      else if(invocation.getParameter() instanceof ComplexObject)
      {
            return OBJECT_RESPONSE_VALUE;
      }
      else if(STRING_RETURN_PARAM.equals(invocation.getParameter()))
      {
         Map responseMetadata = invocation.getReturnPayload();
         responseMetadata.put(HTTPMetadataConstants.RESPONSE_CODE,  new Integer(207));
         responseMetadata.put(HTTPMetadataConstants.RESPONSE_CODE_MESSAGE, "Custom response code and message from remoting server");
         // Just going to return static string as this is just simple example code.
         return RESPONSE_VALUE;
      }
      else
      {
         return HTML_PAGE_RESPONSE;
      }
   }

   /**
    * Adds a callback handler that will listen for callbacks from
    * the server invoker handler.
    *
    * @param callbackHandler
    */
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      // NO OP as do not handling callback listeners in this example
   }

   /**
    * Removes the callback handler that was listening for callbacks
    * from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      // NO OP as do not handling callback listeners in this example
   }

   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
      // NO OP as do not need reference to MBeanServer for this handler
   }

   /**
    * set the invoker that owns this handler
    *
    * @param invoker
    */
   public void setInvoker(ServerInvoker invoker)
   {
      // NO OP as do not need reference back to the server invoker
   }

}