/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.hessian.web;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.jboss.test.remoting.performance.synchronous.CallTracker;
import org.jboss.test.remoting.performance.synchronous.Payload;

import java.util.Map;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHessianServerImpl implements SpringHessianServer
{
   private Map callTrackers = new ConcurrentHashMap();

   public Object sendNumberOfCalls(Object obj, Object param)
   {
      System.out.println("sent number of calls " + obj + " " + param);
      String sessionId = (String) obj;
      Integer totalCountInteger = (Integer) param;
      int totalCount = totalCountInteger.intValue();
      System.out.println("received totalCallCount call with total count of " + totalCount + " from " + sessionId);
      CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
      if (tracker != null)
      {
         tracker.createTotalCount(totalCount);
      }
      else
      {
         SpringHessianHandler callbackHandler = new SpringHessianHandler();
         callbackHandler.start();
         tracker = new CallTracker(sessionId, callbackHandler);
         callTrackers.put(sessionId, tracker);
         tracker.createTotalCount(totalCount);
      }
      return totalCountInteger;
   }

   public Object makeCall(Object obj, Object param)
   {
      Payload payload = (Payload) param;
      int clientInvokeCallCount = payload.getCallNumber();

      String sessionId = (String) obj;
      CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
      if (tracker != null)
      {
         tracker.verifyClientInvokeCount(clientInvokeCallCount);
      }
      else
      {
         System.err.println("No call tracker exists for session id " + sessionId);
         throw new RuntimeException("No call tracker exists for session id " + sessionId);
      }

      return new Integer(clientInvokeCallCount);
   }

}
