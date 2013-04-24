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

package org.jboss.test.remoting.performance.raw.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackServer extends UnicastRemoteObject implements RMICallbackServerRemote, PerformanceCallbackKeeper
{
   private String sessionId;
   private Latch lock;
   private int numberOfCallsProcessed = 0;
   private int numberofDuplicates = 0;

   public RMICallbackServer(String sessionId, Latch lock) throws RemoteException
   {
      super();

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
      //String handbackObj = (String) obj;
      //System.out.println("Handback object should be " + sessionId + " and server called back with " + handbackObj);
      lock.release();
   }


   public void start()
   {
      try
      {
//      if(System.getSecurityManager() == null)
//      {
//         System.setSecurityManager(new RMISecurityManager());
//      }

         int port = 1099;
         Registry registry = LocateRegistry.getRegistry(port);

         String name = "//" + sessionId + "/RMICallbackServer";

         registry.rebind(name, this);
         System.out.println("RMICallbackServer bound");
      }
      catch(Exception e)
      {
         System.err.println("RMICallbackServer exception: " + e.getMessage());
         e.printStackTrace();
      }
   }

   public void finishedProcessing(Object obj) throws RemoteException
   {
      System.out.println("finishedProcessing called with " + obj);
      Callback callback = (Callback) obj;
      try
      {
         handleCallback(callback);
      }
      catch(HandleCallbackException e)
      {
         e.printStackTrace();
      }
   }
}