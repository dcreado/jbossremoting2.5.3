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
package org.jboss.remoting.socketfactory;

import java.io.IOException;
import java.net.Socket;

/** 
 * Interface for a listener that is called when a socket is created.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1866 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public interface SocketCreationListener
{
   /**
    * Called when a socket has been created.
    * 
    * @param socket socket that has been created
    * @param source SocketFactory or ServerSocket that created the socket
    * @throws IOException
    */
   void socketCreated(Socket socket, Object source) throws IOException;
}
