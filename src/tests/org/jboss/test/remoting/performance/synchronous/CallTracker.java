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

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CallTracker
{
   private String clientSessionId;
   private int totalCount = 0;
   private SynchronizedInt duplicateCount = new SynchronizedInt(0);
   private SynchronizedInt receivedCount = new SynchronizedInt(0);

   private boolean[] counterArray = null;

   private InvokerCallbackHandler callbackHandler;

   public CallTracker(String sessionId, InvokerCallbackHandler callbackHandler)
   {
      this.clientSessionId = sessionId;
      this.callbackHandler = callbackHandler;
   }

   public synchronized void createTotalCount(int totalCount)
   {
      counterArray = new boolean[totalCount + 1];
      this.totalCount = totalCount;
      receivedCount.set(0);
      duplicateCount.set(0);
   }

   public void verifyClientInvokeCount(int clientInvokeCallCount)
   {
      boolean duplicate = addToReceivedCount(clientInvokeCallCount);
      if(duplicate)
      {
         duplicateCount.increment();
      }
      else
      {
         receivedCount.increment();
      }

      int currentDuplicateCount = duplicateCount.get();
      int currentReceivedCount = receivedCount.get();
      if((currentReceivedCount % 100) == 0)
      {
         System.out.println(clientSessionId + " -- Received count: " + currentReceivedCount);
         System.out.println(clientSessionId + " -- Duplicate count: " + currentDuplicateCount);
         System.out.println(clientSessionId + " -- Total count: " + totalCount);
      }
      if((currentReceivedCount + currentDuplicateCount) == totalCount)
      {
         System.out.println("\n\n*****************************\n" +
                            "  Test Finished\n" +
                            "*****************************\n" +
                            "  " + clientSessionId + " -- Received Count = " + currentReceivedCount + "\n" +
                            "  " + clientSessionId + " -- Duplicate Count = " + currentDuplicateCount + "\n" +
                            "*****************************\n\n");

         // now call back on client to indicate finished server processing
         if(callbackHandler != null)
         {
            Callback callback = new Callback(new Integer[]{new Integer(currentReceivedCount),
                  new Integer(currentDuplicateCount)});
            try
            {
               callbackHandler.handleCallback(callback);
            }
            catch(HandleCallbackException e)
            {
               e.printStackTrace();
            }
         }
      }

   }

   private synchronized boolean addToReceivedCount(int localClientInvokeCount)
   {
      boolean isDuplicate = false;
      if(counterArray == null)
      {
         System.out.println("Error!  Have not received invoke for method 'totalCallCount', so can not process count.");
         throw new RuntimeException("Error!  Have not received invoke for method 'totalCallCount', so can not process count.");
      }
      else
      {
         try
         {
            isDuplicate = counterArray[localClientInvokeCount];
            if(!isDuplicate)
            {
               counterArray[localClientInvokeCount] = true;
            }
         }
         catch(ArrayIndexOutOfBoundsException e)
         {
            System.err.println("Got ArrayIndexOutOfBoundsException");
            System.err.println("Counter array size = " + counterArray.length);
            System.err.println("Received count = " + localClientInvokeCount);
            e.printStackTrace();
         }
      }
      return isDuplicate;
   }

}