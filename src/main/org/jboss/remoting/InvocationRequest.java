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

package org.jboss.remoting;

import java.io.Serializable;
import java.util.Map;

/**
 * InvocationRequest is passed to ServerInvocationHandler which encapsulates the unmarshalled method
 * invocation parameters from the ServerInvoker.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 1972 $
 */
//TODO: Need to remove Serializable if not going to pass InvocationRequest as the callback object -TME
public class InvocationRequest implements Serializable
{
   static final long serialVersionUID = -6719842238864057289L;

   private String sessionId;

   private String subsystem;
   private Object arg;
   private Map requestPayload;
   private Map returnPayload;
   private InvokerLocator locator;

   public InvocationRequest(String sessionId, String subsystem, Object arg,
                            Map requestPayload, Map returnPayload, InvokerLocator locator)
   {
      this.sessionId = sessionId;
      this.subsystem = subsystem;
      this.arg = arg;
      this.requestPayload = requestPayload;
      this.returnPayload = returnPayload;
      this.locator = locator;
   }

   protected InvocationRequest(Object arg)
   {
      this.arg = arg;
   }

   public InvokerLocator getLocator()
   {
      return locator;
   }

   public void setLocator(InvokerLocator locator)
   {
      this.locator = locator;
   }

   public String getSubsystem()
   {
      return subsystem;
   }

   public void setSubsystem(String subsystem)
   {
      this.subsystem = subsystem;
   }

   public String getSessionId()
   {
      return sessionId;
   }

   public void setSessionId(String sessionId)
   {
      this.sessionId = sessionId;
   }

   public Object getParameter()
   {
      return arg;
   }

   public void setParameter(Object arg)
   {
      this.arg = arg;
   }

   public Map getRequestPayload()
   {
      return requestPayload;
   }

   public void setRequestPayload(Map requestPayload)
   {
      this.requestPayload = requestPayload;
   }

   public Map getReturnPayload()
   {
      return returnPayload;
   }

   public void setReturnPayload(Map returnPayload)
   {
      this.returnPayload = returnPayload;
   }

   public String toString()
   {
      return "InvocationRequest[" + Integer.toHexString(hashCode()) +
         (subsystem != null ? ", " + subsystem : "") +
         (arg != null ? ", " + arg : ", EMPTY") + "]";
   }
}
