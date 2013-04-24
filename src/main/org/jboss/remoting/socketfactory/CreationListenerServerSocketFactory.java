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
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

/** 
 * A CreationListenerServerSocketFactory wraps a ServerSocketFactory, and whenever
 * the ServerSocketFactory creates a ServerSocket, the ServerSocket is wrapped in a
 * CreationListenerServerSocket.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1866 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public class CreationListenerServerSocketFactory
   extends ServerSocketFactory
   implements ServerSocketFactoryWrapper, Serializable
{
   private static final long serialVersionUID = -7939318527267014514L;
   
   private ServerSocketFactory factory;
   private SocketCreationListener listener;
   
   
   public CreationListenerServerSocketFactory(ServerSocketFactory factory,
                                              SocketCreationListener listener)
   {
      this.factory = factory;
      this.listener = listener;
   }
   
   
   public ServerSocketFactory getFactory()
   {
      return factory;
   }
   
   
   public SocketCreationListener getListener()
   {
      return listener;
   }
   
   
   public void setFactory(ServerSocketFactory factory)
   {
      this.factory = factory;
   }
   
   
   public void setListener(SocketCreationListener listener)
   {
      this.listener = listener;
   }
   
   
   public ServerSocket createServerSocket() throws IOException
   {
      ServerSocket serverSocket = factory.createServerSocket();
      return new CreationListenerServerSocket(serverSocket, listener);
   }
   
   
   public ServerSocket createServerSocket(int port) throws IOException
   {
      ServerSocket serverSocket = factory.createServerSocket(port);
      return new CreationListenerServerSocket(serverSocket, listener);
   }

   
   public ServerSocket createServerSocket(int port, int backlog) throws IOException
   {
      ServerSocket serverSocket = factory.createServerSocket(port, backlog);
      return new CreationListenerServerSocket(serverSocket, listener);
   }

   
   public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
   throws IOException
   {
      ServerSocket serverSocket = factory.createServerSocket(port, backlog, ifAddress);
      return new CreationListenerServerSocket(serverSocket, listener);
   }


   public ServerSocketFactory getServerSocketFactory()
   {
      return factory;
   }


   public void setServerSocketFactory(ServerSocketFactory factory)
   {
      this.factory = factory;
   }
}
