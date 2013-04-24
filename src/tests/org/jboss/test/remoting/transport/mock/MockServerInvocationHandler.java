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

package org.jboss.test.remoting.transport.mock;

import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.test.remoting.ComplexReturn;
import org.jboss.test.remoting.byvalue.ByValuePayload;

/**
 * MockServerInvocationHandler
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 1066 $
 */
public class MockServerInvocationHandler implements ServerInvocationHandler
{
   private ServerInvoker invoker;
   private List listeners = Collections.synchronizedList(new ArrayList());
   private Map clientListeners = new HashMap();

   private static final Logger log = Logger.getLogger(MockServerInvocationHandler.class);


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

      if(param instanceof NameBasedInvocation)
      {
         NameBasedInvocation nbi = (NameBasedInvocation) param;
         methodName = nbi.getMethodName();
         params = nbi.getParameters();
         sig = nbi.getSignature();
      }
      else if(param instanceof InternalInvocation)
      {
         InternalInvocation ii = (InternalInvocation) param;
         methodName = ii.getMethodName();
         params = ii.getParameters();
      }
      else
      {
         log.info("Don't recognize the parameter type, so just returning it.");
         return param;
      }

      String sessionId = invocation.getSessionId();
      String subsystem = invocation.getSubsystem();

      log.debug("invoke() called with method: " + methodName +
                "\tsessionId: " + sessionId + "\tsubsystem:" + subsystem);
      //deprecated since specific to JMX (old way of handling callbacks)
      if(methodName.equals("testComplexReturn"))
      {
         //Need to send back complex object containing array of complext objects.
         ComplexReturn ret = new ComplexReturn();
         return ret;
      }
      if(methodName.equals("testMarshalledObject"))
      {
         ComplexReturn ret = new ComplexReturn();
         MarshalledObject mObj = new MarshalledObject(ret);
         return mObj;
      }
      else if(methodName.equals("test"))
      {
         // will cause a callback on all the listeners
         log.debug("test called on server invocation handler, so should do callback.");
         CallbackDispatcher callbackDispatcher = new CallbackDispatcher(invocation.getSessionId(),
                                                                        invocation.getSubsystem(),
                                                                        new NameBasedInvocation("handleCallback",
                                                                                                params,
                                                                                                sig));
         Thread callbackThread = new Thread(callbackDispatcher);
         callbackThread.start();
      }
      else if(methodName.equals("addClientListener"))
      {
         Object obj = params[0];
         InvokerCallbackHandler clientHandler = (InvokerCallbackHandler) obj;

         clientListeners.put(invocation.getSessionId(), clientHandler);
      }
      else if(methodName.equals("removeClientListener"))
      {
         Object obj = params[0];
         InvokerCallbackHandler clientHandler = (InvokerCallbackHandler) obj;

         clientListeners.remove(invocation.getSessionId());
      }
      else if(methodName.equals("handleCallback"))
      {
         // got a callback from remote server
         InvokerCallbackHandler clientHandler = (InvokerCallbackHandler) clientListeners.get(sessionId);
         clientHandler.handleCallback(new Callback(invocation.getParameter()));
      }
      else if(methodName.equals("testException") || methodName.equals("testThrowException"))
      {
         throw new Exception("Got call from client to throw exception.  This is expected.");
      }
      else if(methodName.equals("testByValue"))
      {
         // check to see if by value payload was serialized at some point
         Object arg = params[0];
         if(arg instanceof ByValuePayload)
         {
            ByValuePayload byValuePayload = (ByValuePayload) arg;
            return new Boolean(byValuePayload.wasMarshalled());
         }
         else
         {
            // Error in tests
            return Boolean.FALSE;
         }
      }
      Object ret = null;
      if(params != null)
      {
         ret = params[0];
         log.info("Found a parameter to return " + ret);
      } // end of if ()
      else
      {
         log.info("returning null");
      } // end of else

      return ret;
   }

   /**
    * @param sessionId
    * @deprecated
    */
   private void handleRemoveNotificationListener(String sessionId)
   {
      listeners.remove(sessionId);
   }

   /**
    * @param clientLocator
    * @param subsystem
    * @param sessionId
    * @throws Exception
    * @deprecated
    */
   private void handleAddNotificationListener(InvokerLocator clientLocator,
                                              String subsystem,
                                              String sessionId) throws Exception
   {
      Client callBackClient = new Client(clientLocator, subsystem);
      callBackClient.connect();
      listeners.add(callBackClient);
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

   private class CallbackDispatcher implements Runnable
   {
      private String sessionId;
      private String subsystem;
      private Object param;

      public CallbackDispatcher(String sessionId, String subsystem, Object param)
      {
         this.sessionId = sessionId;
         this.subsystem = subsystem;
         this.param = param;
      }

      public void run()
      {
         List tempList = null;
         synchronized(listeners)
         {
            tempList = new ArrayList(listeners);
         }
         
         Iterator itr = tempList.iterator();
         while(itr.hasNext())
         {
            try
            {
               InvokerCallbackHandler handler = (InvokerCallbackHandler) itr.next();
               Callback invocation = new Callback(param);
               handler.handleCallback(invocation);
            }
            catch(Throwable e)
            {
               e.printStackTrace();
            }

         }
      }
   }
}
