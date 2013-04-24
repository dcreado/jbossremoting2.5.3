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
package org.jboss.remoting.callback;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Version;

import java.util.HashMap;
import java.util.Map;


/**
 * This is the class to use for sending callback payloads from the
 * server handler to the callback listener.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Callback extends InvocationRequest
{
   static final long serialVersionUID;

   public final static String CALLBACK_HANDLE_OBJECT_KEY = "callback_handle_object";
   public final static String SERVER_LOCATOR_KEY = "server_locator";

   static
   {
      if(Version.getDefaultVersion() == Version.VERSION_1)
      {
         serialVersionUID = -4778964132014467531L;
      }
      else
      {
         serialVersionUID = -9196653590434634454L;
      }
   }

   /**
    * Constructs the callback object with any object payload.
    *
    * @param callbackPayload
    */
   public Callback(Object callbackPayload)
   {
      super(callbackPayload);
   }

   /**
    * Returns the handle object originally specified when initially registering
    * the callback listener.  This can be used to provide context upon callbacks.
    *
    * @return
    */
   public Object getCallbackHandleObject()
   {
      Object handleObject = null;
      Map returnPayload = getReturnPayload();
      if(returnPayload != null)
      {
         handleObject = returnPayload.get(CALLBACK_HANDLE_OBJECT_KEY);
      }
      return handleObject;
   }

   protected void setCallbackHandleObject(Object handleObject)
   {
      Map returnPayload = getReturnPayload();
      if(returnPayload == null)
      {
         returnPayload = new HashMap();
         setReturnPayload(returnPayload);
      }

      returnPayload.put(CALLBACK_HANDLE_OBJECT_KEY, handleObject);
   }


   /**
    * Gets the callback payload sent from the server handler.
    *
    * @return
    */
   public Object getCallbackObject()
   {
      return getParameter();
   }

   /**
    * Gets the locator for the target server where the callback was generated (this will be for
    * the same server that the callback client was registered).
    *
    * @return
    */
   public InvokerLocator getServerLocator()
   {
      InvokerLocator locator = null;
      Map returnPayload = getReturnPayload();
      if(returnPayload != null)
      {
         locator = (InvokerLocator) returnPayload.get(SERVER_LOCATOR_KEY);
      }
      return locator;
   }
}