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

package org.jboss.remoting.stream;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;

import java.io.InputStream;

/**
 * This interface is intended for those handlers that expect to
 * receive calls from clients where a InputStream will be passed.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public interface StreamInvocationHandler extends ServerInvocationHandler
{
   /**
    * Will receive an input stream, which is actually a proxy back
    * to the original stream on the client's vm.  This stream can
    * be acted upon as though it was a local input stream.
    * When finished reading from the stream, it MUST be closed, or
    * will remain open on the client side.
    *
    * @param stream stream proxy to client's original stream
    * @param param  the payload associated with the invocation
    */
   public Object handleStream(InputStream stream, InvocationRequest param) throws Throwable;
}