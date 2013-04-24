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

package org.jboss.remoting.invocation;

import java.io.Serializable;

/**
 * Serves as the base invocation object for the different types of remoting invocations.
 * All remoting invocations should extend this class (i.e. InternalInvocation, NameBasedInvocation, etc.)
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class RemoteInvocation implements Serializable
{
   static final long serialVersionUID = -5420149048705763388L;

   protected final String methodName;
   protected final Object[] params;

   public RemoteInvocation(final String methodName, final Object[] params)
   {
      this.methodName = methodName;
      this.params = params;
   }

   public String getMethodName()
   {
      return methodName;
   }

   public Object[] getParameters()
   {
      return params;
   }
}
