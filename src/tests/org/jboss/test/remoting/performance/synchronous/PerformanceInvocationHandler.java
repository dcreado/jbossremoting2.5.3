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

package org.jboss.test.remoting.performance.synchronous;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.SyncList;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceInvocationHandler implements ServerInvocationHandler
{
   private ServerInvoker invoker;

   private List listeners = new SyncList(new ArrayList(), new FIFOReadWriteLock());
   private Map callTrackers = new ConcurrentHashMap();

   private static final Logger log = Logger.getLogger(PerformanceInvocationHandler.class);


   /**
    * set the mbean server that the handler can reference
    *
    * @param server
    */
   public void setMBeanServer(MBeanServer server)
   {
   }

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
      Object param = invocation.getParameter();
      String sessionId = invocation.getSessionId();
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
      if(methodName.equals(PerformanceClientTest.NUM_OF_CALLS))
      {
         Integer totalCountInteger = (Integer) params[0];
         int totalCount = totalCountInteger.intValue();
         System.out.println("received totalCallCount call with total count of " + totalCount + " from " + sessionId);
         CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
         if(tracker != null)
         {
            tracker.createTotalCount(totalCount);
            ret = totalCountInteger;
         }
         else
         {
            log.error("Calling " + methodName + " but no call tracker exists for session id " + sessionId);
            throw new Exception("Calling " + methodName + " but no call tracker exists for session id " + sessionId);
         }
      }
      else if(methodName.equals(PerformanceClientTest.TEST_INVOCATION))
      {
         if(params != null)
         {
            Payload payload = (Payload) params[0];
            //System.out.println(payload);
            int clientInvokeCallCount = payload.getCallNumber();

            CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
            if(tracker != null)
            {
               tracker.verifyClientInvokeCount(clientInvokeCallCount);
            }
            else
            {
               log.error("Calling " + methodName + " but no call tracker exists for session id " + sessionId);
               throw new Exception("Calling " + methodName + " but no call tracker exists for session id " + sessionId);
            }
            // just passing return, even though not needed
            ret = new Integer(clientInvokeCallCount);
         }
         else
         {
            log.error("no parameter passed for method call " + methodName);
         }

      }
      else
      {
         throw new Exception("Don't know what to do with call to " + methodName);
      }

      return ret;

   }

   private void createCallTracker(String sessionId, InvokerCallbackHandler callbackHandler)
   {
      CallTracker tracker = new CallTracker(sessionId, callbackHandler);
      callTrackers.put(sessionId, tracker);
   }

   /**
    * Adds a callback handler that will listen for callbacks from
    * the server invoker handler.
    *
    * @param callbackHandler
    */
   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      ServerInvokerCallbackHandler handler = (ServerInvokerCallbackHandler) callbackHandler;
      String sessionId = handler.getClientSessionId();
      System.out.println("Adding callback listener.  Callback handler has session id: " + sessionId);
      createCallTracker(sessionId, callbackHandler);
      listeners.add(callbackHandler);
   }

   /**
    * Removes the callback handler that was listening for callbacks
    * from the server invoker handler.
    *
    * @param callbackHandler
    */
   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      //TODO: -TME Need to figure out how this should be handled.
      // Could look up CallTracker based on session id (as in addListener() method)
      // and then remove from tracker or kill tracker all together.
      listeners.remove(callbackHandler);
   }
}