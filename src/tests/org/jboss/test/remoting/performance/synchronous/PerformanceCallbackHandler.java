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

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceCallbackHandler implements InvokerCallbackHandler, PerformanceCallbackKeeper
{
   private String sessionId;
   private Latch lock;
   private int numberOfCallsProcessed = 0;
   private int numberofDuplicates = 0;

   public PerformanceCallbackHandler(String sessionId, Latch lock)
   {
      this.sessionId = sessionId;
      this.lock = lock;
   }

   public int getNumberOfCallsProcessed()
   {
      return numberOfCallsProcessed;
   }

   public int getNumberOfDuplicates()
   {
      return numberofDuplicates;
   }

   /**
    * Will take the callback message and send back to client.
    * If client locator is null, will store them till client polls to get them.
    *
    * @param callback
    * @throws org.jboss.remoting.callback.HandleCallbackException
    *
    */
   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      Object ret = callback.getCallbackObject();
      Integer[] handledArray = (Integer[]) ret;
      Integer numOfCallsHandled = (Integer) handledArray[0];
      Integer numOfDuplicates = (Integer) handledArray[1];
      System.out.println("Server is done.  Number of calls handled: " + numOfCallsHandled);
      numberOfCallsProcessed = numOfCallsHandled.intValue();
      System.out.println("Number of duplicate calls: " + numOfDuplicates);
      numberofDuplicates = numOfDuplicates.intValue();
      Object obj = callback.getCallbackHandleObject();
      String handbackObj = (String) obj;
      System.out.println("Handback object should be " + sessionId + " and server called back with " + handbackObj);
      lock.release();
   }
}
