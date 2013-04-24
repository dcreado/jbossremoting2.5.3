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
package org.jboss.remoting.transport;

import org.jboss.remoting.InvokerLocator;

import java.util.Map;

/**
 * This interface is used to indicate that a client invoker represents
 * as transport that is bidirectional, which means that calls from the
 * server to the client can be made without having to open a new physical connection
 * from the server back to the client/
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public interface BidirectionalClientInvoker extends ClientInvoker
{
   /**
    * Gets the locator to be used for callbacks when want do not want
    * to establish a new physical connectiong from the server to the client.
    * @param metadata
    * @return
    */
   public InvokerLocator getCallbackLocator(Map metadata);
}