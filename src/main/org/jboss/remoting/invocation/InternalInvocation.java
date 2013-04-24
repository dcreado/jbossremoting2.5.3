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


/**
 * InternalInvocation.java is an invocation object for use in the
 * remoting layer for callbacks etc.  We are responsible for assuring
 * that each method name has a unique arg list.
 * <p/>
 * <p/>
 * Created: Mon Apr 28 09:14:46 2003
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version 1.0
 */
public class InternalInvocation extends RemoteInvocation

{
   static final long serialVersionUID = 2629656457294678240L;

   public static final String ADDCLIENTLISTENER = "addClientListener";
   public static final String ADDLISTENER = "addListener";
   public static final String REMOVELISTENER = "removeListener";
   public static final String REMOVECLIENTLISTENER = "removeClientListener";
   public static final String GETCALLBACKS = "getCallbacks";
   public static final String HANDLECALLBACK = "handleCallback";
   public static final String ADDSTREAMCALLBACK = "addStreamCallback";
   public static final String ACKNOWLEDGECALLBACK = "acknowledgeCallback";
   public static final String ECHO = "echo";

   public InternalInvocation(final String methodName, final Object[] params)
   {
      super(methodName, params);
   }

   public String toString()
   {
      return "InternalInvocation[" + Integer.toHexString(hashCode()) + "]";
   }
}
