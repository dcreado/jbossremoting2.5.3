/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.client;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHttpCallbackServerImpl implements SpringHttpCallbackServer
{
   private String sessionId;
   private Latch lock;
   private int numberOfCallsProcessed = 0;
   private int numberOfDuplicates = 0;


   public void finishedProcessing(Object obj)
   {
      System.out.println("finishedProcessing called with " + obj);
      Callback callback = (Callback)obj;
      try
      {
         handleCallback(callback);
      }
      catch(HandleCallbackException e)
      {
         e.printStackTrace();
      }
   }

   public void setClientSessionId(String clientSessionId)
   {
      this.sessionId = clientSessionId;
   }

   public void setServerDoneLock(Latch serverDoneLock)
   {
      this.lock = serverDoneLock;
   }

   public int getNumberOfCallsProcessed()
   {
      return numberOfCallsProcessed;
   }

   public int getNumberOfDuplicates()
   {
      return numberOfDuplicates;
   }

   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      Object ret = callback.getCallbackObject();
      Integer[] handledArray = (Integer[])ret;
      Integer numOfCallsHandled = (Integer) handledArray[0];
      Integer numOfDuplicates = (Integer) handledArray[1];
      System.out.println("Server is done.  Number of calls handled: " + numOfCallsHandled);
      numberOfCallsProcessed = numOfCallsHandled.intValue();
      System.out.println("Number of duplicate calls: " + numOfDuplicates);
      numberOfDuplicates = numOfDuplicates.intValue();
      Object obj = callback.getCallbackHandleObject();
      //String handbackObj = (String) obj;
      //System.out.println("Handback object should be " + sessionId + " and server called back with " + handbackObj);
      lock.release();

   }

}
