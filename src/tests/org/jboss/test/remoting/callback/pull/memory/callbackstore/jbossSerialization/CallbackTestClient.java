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

package org.jboss.test.remoting.callback.pull.memory.callbackstore.jbossSerialization;

import org.jboss.remoting.InvokerLocator;

/**
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.org">Ron Sigal</a>
 */
public class CallbackTestClient extends org.jboss.test.remoting.callback.pull.memory.callbackstore.CallbackTestClient
{
   protected String getInvokerLocatorURI()
   {
      String baseLocatorURI = super.getInvokerLocatorURI();
      if (baseLocatorURI.indexOf("/?") == -1)
         return baseLocatorURI + "/?" + InvokerLocator.SERIALIZATIONTYPE + "=jboss";
      else
         return baseLocatorURI + "&" + InvokerLocator.SERIALIZATIONTYPE + "=jboss";
   }

}
