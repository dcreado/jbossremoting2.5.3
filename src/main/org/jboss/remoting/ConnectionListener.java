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

/**
 * Listener that can be registered with the Client to receive a callback if the target server for 
 * said Client is determined to be unreachable at any point.
 * <p>
 * Can also be registered with Connector to be notified when client disconnects (but only when
 * leasing is turned on).
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public interface ConnectionListener
{
   /**
    * Called when a target server or client found to be dead.
    *
    * @param throwable - original exception thrown when trying to connect to target server. If is
    *        listener on server for client failure, the exception will be a
    *        ClientDisconnectedException if the client disconnected normally, or null if the lease
    *        expired.
    * @param client - the client from which this call was made.
    */
   public void handleConnectionException(Throwable throwable, Client client);
}