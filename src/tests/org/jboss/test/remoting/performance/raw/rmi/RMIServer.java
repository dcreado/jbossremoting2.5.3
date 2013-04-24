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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.jboss.test.remoting.performance.synchronous.CallTracker;
import org.jboss.test.remoting.performance.synchronous.Payload;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMIServer extends UnicastRemoteObject implements RMIServerRemote
{
   private static final Logger log = Logger.getLogger(RMIServer.class);

   private Map callTrackers = new ConcurrentHashMap();

   private String host = "localhost";

   public RMIServer(String host) throws RemoteException
   {
      super();
      this.host = host;
   }

   public Object sendNumberOfCalls(Object obj, Object param) throws RemoteException
   {
      System.out.println("sent number of calls " + obj + " " + param);
      String sessionId = (String) obj;
      Integer totalCountInteger = (Integer) param;
      int totalCount = totalCountInteger.intValue();
      System.out.println("received totalCallCount call with total count of " + totalCount + " from " + sessionId);
      CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
      if(tracker != null)
      {
         tracker.createTotalCount(totalCount);
      }
      else
      {
         RMIHandler callbackHandler = new RMIHandler(host, sessionId);
         tracker = new CallTracker(sessionId, callbackHandler);
         callTrackers.put(sessionId, tracker);
         tracker.createTotalCount(totalCount);
      }
      return totalCountInteger;
   }

   public Object makeCall(Object obj, Object param) throws RemoteException
   {
      Payload payload = (Payload) param;
      //System.out.println(payload);
      int clientInvokeCallCount = payload.getCallNumber();

      String sessionId = (String) obj;
      CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
      if(tracker != null)
      {
         tracker.verifyClientInvokeCount(clientInvokeCallCount);
      }
      else
      {
         log.error("No call tracker exists for session id " + sessionId);
         throw new RemoteException("No call tracker exists for session id " + sessionId);
      }
      // just passing return, even though not needed
      return new Integer(clientInvokeCallCount);
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
         Registry registry = LocateRegistry.createRegistry(port);

         String name = "//" + host + "/RMIServer";

         RMIServerRemote engine = new RMIServer(host);
//         Naming.rebind(name, engine);
         registry.rebind(name, this);
         System.out.println("RMIServer bound");
      }
      catch(Exception e)
      {
         System.err.println("RMIServer exception: " + e.getMessage());
         e.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      try
      {
         String host = "localhost";
         RMIServer server = new RMIServer(host);
         server.start();
      }
      catch(RemoteException e)
      {
         e.printStackTrace();
      }
   }
}