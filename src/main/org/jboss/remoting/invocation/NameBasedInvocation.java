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

import java.lang.reflect.Method;

/**
 * NameBasedInvocation.java is an invocation object in jmx style.
 * <p/>
 * <p/>
 * Created: Mon Apr 28 09:14:46 2003
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @version 1.0
 */
public class NameBasedInvocation extends RemoteInvocation
{
   /**
    * @since 4.0.1
    */
   static final long serialVersionUID = -6507163932605308471L;

   private final String[] sig;

   public NameBasedInvocation(final Method method, final Object[] params)
   {
      super(method.getName(), params);
      sig = generateSignatureFromMethod(method);
   }

   private String[] generateSignatureFromMethod(final Method method)
   {
      Class[] parameterTypes = method.getParameterTypes();
      String[] signature = new String[parameterTypes.length];
      for(int i = 0; i < parameterTypes.length; i++)
      {
         Class parameterType = parameterTypes[i];
         signature[i] = parameterType.getName();
      }
      return signature;
   }

   public NameBasedInvocation(final String methodName, final Object[] params, final String[] sig)
   {
      super(methodName, params);
      this.sig = sig;
   } // NameBasedInvocation constructor

   public String[] getSignature()
   {
      return sig;
   }

   public String toString()
   {
      return "NameBasedInvocation[" + methodName + "]";
   }

} // NameBasedInvocation
