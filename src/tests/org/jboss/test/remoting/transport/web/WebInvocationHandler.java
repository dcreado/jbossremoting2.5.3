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

package org.jboss.test.remoting.transport.web;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class WebInvocationHandler implements ServerInvocationHandler
{
   // String to be returned from invocation handler upon client invocation calls.
   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";
   public static final ComplexObject OBJECT_RESPONSE_VALUE = new ComplexObject(5, "dub", false);
   public static final ComplexObject LARGE_OBJECT_RESPONSE_VALUE = new ComplexObject(5, "dub", false, 7568);

   public static final String NULL_RETURN_PARAM = "return_null";
   public static final String OBJECT_RETURN_PARAM = "return_object";
   public static final String THROW_EXCEPTION_PARAM = "throw_exception";
   public static final String STRING_RETURN_PARAM = "return_string";
   public static final String USER_AGENT_PARAM = "user_agent";
   public static final String HTML_PAGE_RESPONSE = "<html><head><title>Test HTML page</title></head><body>" +
                                                   "<h1>HTTP/Servlet Test HTML page</h1><p>This is a simple page served for test." +
                                                   "<p>Should show up in browser or via invoker client</body></html>";
   public static final String SET_CONTENT_TYPE = "setContentType";
   public static final String CONTENT_TYPE = "test/testContentType";
   
   public static final String GET_ADDRESS = "getAddress";
   public static final String OPEN_CONNECTION = "openConnection";
   public static final String SEND_CALLBACK = "sendCallback";
   public static final String COPY = "copy:";
   public static final int ANSWER = 17;
   
   public static final String CHECK_MBEAN_SERVER = "checkMBeanServer";
   public static final String DEFAULT_DOMAIN = "defaultDomain";
   
   protected static String HEADER_RESPONSE_KEY = "responseKey";
   protected static String HEADER_RESPONSE_VALUE = "responseValue";
   protected static String RETURN_RESPONSE_HEADER = "returnResponseHeader";
   
   private static Logger log = Logger.getLogger(WebInvocationHandler.class);
   
   private InvokerCallbackHandler callbackHandler;
   private MBeanServer server;

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
      Object request = invocation.getParameter();
      log.debug("Invocation request is: " + request);
      if(NULL_RETURN_PARAM.equals(request))
      {
         return null;
      }
      else if(THROW_EXCEPTION_PARAM.equals(request))
      {
         log.debug("throwing WebTestException");
         throw new WebTestException("This is an exception being thrown as part of test case.  It is intentional.");
      }
      else if(request instanceof ComplexObject)
      {
         ComplexObject obj = (ComplexObject) request;
         if(obj.getSize() > 1024)
         {
            return LARGE_OBJECT_RESPONSE_VALUE;
         }
         else
         {
            return OBJECT_RESPONSE_VALUE;
         }
      }
      else if(STRING_RETURN_PARAM.equals(request))
      {
         // Just going to return static string as this is just simple example code.
         return RESPONSE_VALUE;
      }
      else if(USER_AGENT_PARAM.equals(request))
      {
         // return user agent found in map
         return invocation.getRequestPayload().get("user-agent");
      }
      else if (SET_CONTENT_TYPE.equals(request))
      {
         Map returnPayload = invocation.getReturnPayload();
         if (returnPayload == null)
         {
            returnPayload = new HashMap();
            invocation.setReturnPayload(returnPayload);
         }
         returnPayload.put("Content-Type", CONTENT_TYPE);
         return CONTENT_TYPE;
      }
      else if (GET_ADDRESS.equals(request))
      {
         InetAddress address = (InetAddress) invocation.getRequestPayload().get(Remoting.CLIENT_ADDRESS);
         log.info("returning address: " + address);
         return address;
      }
      else if (OPEN_CONNECTION.equals(request))
      {
         InetAddress addr = (InetAddress) invocation.getRequestPayload().get(Remoting.CLIENT_ADDRESS);
         log.info("creating socket connected to: " + addr);
         Integer callbackPortInt = (Integer) invocation.getRequestPayload().get("callbackPort");
         int callbackPort = callbackPortInt.intValue();
         Socket s = new Socket(addr, callbackPort);
         log.info("created socket connected to: " + addr);
         OutputStream os = s.getOutputStream();
         os.write(ANSWER);
         log.info("wrote answer");
         s.close();
         return null;
      }
      else if (SEND_CALLBACK.equals(request))
      {
         callbackHandler.handleCallback(new Callback("callback"));
         return null;
      }
      else if (request instanceof String && ((String)request).startsWith(COPY))
      {
         return ((String) invocation.getParameter()).substring(5);
      }
      else if (RETURN_RESPONSE_HEADER.equals(request))
      {
         Map returnPayload = invocation.getReturnPayload();
         if (returnPayload == null)
         {
            returnPayload = new HashMap();
            invocation.setReturnPayload(returnPayload);
         }
         returnPayload.put(HEADER_RESPONSE_KEY, HEADER_RESPONSE_VALUE);
         return request;
      }
      else if (CHECK_MBEAN_SERVER.equals(request))
      {
         log.info("MBeanServer: " + server);
         Map metadata = invocation.getRequestPayload();
         String defaultDomain = (String) metadata.get(DEFAULT_DOMAIN);
         log.info("defaultDomain: " + defaultDomain);
         
         if ("jboss".equals(defaultDomain))
            return new Boolean("jboss".equals(server.getDefaultDomain()));
         else if ("platform".equals(defaultDomain))
            return new Boolean(!("jboss".equals(server.getDefaultDomain()))); 
         else
            return new Boolean(false);
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
      this.callbackHandler = callbackHandler;
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
      this.server = server;
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