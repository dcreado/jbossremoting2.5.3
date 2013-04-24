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
package org.jboss.test.remoting.transport.socket.socketpool;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.transport.socket.ClientSocketWrapper;

/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Nov 30, 2007
 * </p>
 */
public class SocketWrapperWithSocketException extends ClientSocketWrapper
{

   public SocketWrapperWithSocketException(Socket socket, Map metadata, Integer timeout) throws Exception
   {
      super(socket, metadata, timeout);  
   }
   
   protected InputStream createInputStream(String serializationType, Socket socket, UnMarshaller unmarshaller)
   throws IOException
   {
      return new ExplodingInputStream();
   }
   
   public static class ExplodingInputStream extends InputStream
   {
      public int read() throws IOException
      {
         throw new SocketException("bang");
      }  
   }
}

