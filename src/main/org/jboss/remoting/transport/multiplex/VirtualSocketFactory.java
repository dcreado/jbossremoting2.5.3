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

/*
 * Created on Jul 24, 2005
 */
 
package org.jboss.remoting.transport.multiplex;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * <code>VirtualSocketFactory</code> extends the
 * <code>javax.net.SocketFactory</code> class and reimplements
 * its <code>createSocket()</code> methods, returning <code>VirtualSocket</code>s.  
 * 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class VirtualSocketFactory extends SocketFactory implements Serializable
{
   private static final long serialVersionUID = 3422810508320563967L;

/**
 * See superclass javadoc.
 */
      public static SocketFactory getDefault()
      {
         return new VirtualSocketFactory();
      }
   
   
/**
 * See superclass javadoc.
 */
   protected VirtualSocketFactory()
   {
   }

   
/**
 * See superclass javadoc.
 */
   public Socket createSocket()
   {
      return new VirtualSocket();
   }
   
   
/** 
 * See superclass javadoc.
 */
   public Socket createSocket(String host, int port) throws IOException, UnknownHostException
   {
      return new VirtualSocket(host, port);
   }

   
/**
 * See superclass javadoc.
 */
   public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
   throws IOException, UnknownHostException
   {
      return new VirtualSocket(host, port, localAddress, localPort);
   }

   
/**
 * See superclass javadoc.
 */
   public Socket createSocket(InetAddress address, int port) throws IOException
   {
      return new VirtualSocket(address, port);
   }

   
/**
 * See superclass javadoc.
 */
   public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
   throws IOException, UnknownHostException
   {
      return new VirtualSocket(address, port, localAddress, localPort);
   }
}
