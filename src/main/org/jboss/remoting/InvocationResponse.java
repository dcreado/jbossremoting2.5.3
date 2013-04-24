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
 * InvocationResponse is a return object from a call to a remote Server Invoker.
 * The InvocationResponse may contain either an Exception or a result value (which may be null in
 * the case the user returns null)
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 1972 $
 */
public class InvocationResponse implements Serializable
{
   static final long serialVersionUID = 1324503813652865685L;

   private final String sessionId;
   private final boolean isException;
   private final Object result;
   private Map payload;

   public InvocationResponse(String sessionId, Object result, boolean isException, Map payload)
   {
      this.sessionId = sessionId;
      this.isException = isException;
      this.result = result;
      this.payload = payload;
   }

   public String getSessionId()
   {
      return sessionId;
   }

   public Map getPayload()
   {
      return payload;
   }

   public boolean isException()
   {
      return isException;
   }

   public Object getResult()
   {
      return result;
   }

   public String toString()
   {
      return "InvocationResponse[" + Integer.toHexString(hashCode()) + ", " +
         (result == null ? "EMPTY" : result) + "]";
   }

}
