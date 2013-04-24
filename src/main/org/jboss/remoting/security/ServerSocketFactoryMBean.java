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
package org.jboss.remoting.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * This interface is due to the constraint added by using JMX based configuration.
 * If want to inject a custom server socket factory (or the one provided by JBossRemoting),
 * it has to implement this interface.  This is due to the fact that concrete objects can not
 * be injected at this point, only interfaces (and the java language ServerSocketFactory
 * class does not implement any interface).
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface ServerSocketFactoryMBean
{
   /**
    * Returns an unbound server socket. The socket is configured with the socket
    * options (such as accept timeout) given to this factory.
    *
    * @return
    * @throws IOException
    */
   ServerSocket createServerSocket() throws IOException;

   /**
    * Returns a server socket bound to the specified port. The socket is configured
    * with the socket options (such as accept timeout) given to this factory.
    *
    * @param i
    * @return
    * @throws IOException
    */
   ServerSocket createServerSocket(int i) throws IOException;

   /**
    * Returns a server socket bound to the specified port,
    * and uses the specified connection backlog. The socket is configured
    * with the socket options (such as accept timeout) given to this factory.
    *
    * @param i
    * @param i1
    * @return
    * @throws IOException
    */
   ServerSocket createServerSocket(int i, int i1) throws IOException;

   /**
    * Returns a server socket bound to the specified port, with a specified
    * listen backlog and local IP. The bindAddr argument can be used on a multi-homed
    * host for a ServerSocket that will only accept connect requests to one of its addresses.
    * The socket is configured with the socket options (such as accept timeout) given to this factory.
    *
    * @param i
    * @param i1
    * @param inetAddress
    * @return
    * @throws IOException
    */
   ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException;
}
