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

import java.io.Serializable;
import java.util.List;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MockInvokerCallbackHandler implements InvokerCallbackHandler, Serializable
{
   private String callbackId;
   private int callbacksRecieved = 0;

   public MockInvokerCallbackHandler(String callbackId)
   {
      this.callbackId = callbackId;
   }

   /**
    * Will take the callback message and send back to client.
    * If client locator is null, will store them till client polls to get them.
    *
    * @param callback
    * @throws org.jboss.remoting.callback.HandleCallbackException
    *
    */
   public void handleCallback(Callback callback)
         throws HandleCallbackException
   {
      System.err.println("We got callback on client.  " + callback + " for " + this);
      this.callbacksRecieved++;
   }

   public int isCallbackReceived()
   {
      System.err.println("returning " + callbacksRecieved + " for callback recieved for " + this);
      return this.callbacksRecieved;
   }

   //TODO: Important that client caller keeps id unique and maintains id
   // since used as key when add/remove listener in client subsystem handler -TME
   public String getId()
   {
      return callbackId;
   }

   /**
    * Will get current list of callbacks.
    *
    * @return
    */
   //TODO: This is messed up.  Why should client InvokerCallbackHandler have to implement this?
   // should probably make parent interface that does not have this one -TME
   public List getCallbacks()
   {
      return null;
   }

   /**
    * This method is required to be called upon removing a callback listener
    * so can clean up resources used by the handler.  In particular, should
    * call disconnect on internal Client.
    */
   public void destroy()
   {
   }
}
