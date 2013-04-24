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

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMIHandler implements InvokerCallbackHandler
{
   private RMICallbackServerRemote callbackServer;

   public RMIHandler(String host, String sessionId)
   {
      int rmiPort = 1099;
      String name = "//" + sessionId + "/RMICallbackServer";
//         RMIServer svr = (RMIServer) Naming.lookup(name);

      try
      {
         Registry regsitry = LocateRegistry.getRegistry(host, rmiPort);
         Remote remoteObj = regsitry.lookup(name);
         callbackServer = (RMICallbackServerRemote) remoteObj;
      }
      catch(RemoteException e)
      {
         e.printStackTrace();
      }
      catch(NotBoundException e)
      {
         e.printStackTrace();
      }

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
      System.out.println("Need to make call on RMICallbackServer with results. " + callback);

      try
      {
         callbackServer.finishedProcessing(callback);
      }
      catch(RemoteException e)
      {
         e.printStackTrace();
         throw new HandleCallbackException(e.getMessage());
      }
   }
}