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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.remoting.util.SecurityUtility;

/** 
 * A CreationListenerServerSocket wraps a ServerSocket to which it delegates
 * calls to accept(), and when the wrapped ServerSocket creates a Socket in 
 * accept(), a SocketCreationListener is notified before the Socket is returned.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 5010 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public class CreationListenerServerSocket extends ServerSocket
{
   private ServerSocket serverSocket;
   private SocketCreationListener listener;
   private PrivilegedExceptionAction action;

   
   public CreationListenerServerSocket(ServerSocket serverSocket, SocketCreationListener listener)
   throws IOException
   {
      this.serverSocket = serverSocket;
      this.listener = listener;
      
      action = new PrivilegedExceptionAction()
      {
         public Object run() throws Exception
         {
             return CreationListenerServerSocket.this.serverSocket.accept();
         }
      };
   }

  
   public SocketCreationListener getListener()
   {
      return listener;
   }
   
   
   public ServerSocket getServerSocket()
   {
      return serverSocket;
   }
   
   
   public void setListener(SocketCreationListener listener)
   {
      this.listener = listener;
   }
   
   
   public void setServerSocket(ServerSocket serverSocket)
   {
      this.serverSocket = serverSocket;
   }
   
   
   public void bind(SocketAddress endpoint) throws IOException
   {
      bind(serverSocket, endpoint);
   }
   
   
   public void bind(SocketAddress endpoint, int backlog) throws IOException
   {
      bind(serverSocket, endpoint, backlog);
   }
   
   
   public Socket accept() throws IOException
   {  
      Socket socket = null;
      
      if (SecurityUtility.skipAccessControl())
      {
         socket = serverSocket.accept();
      }
      else
      {
         try
         {
            socket = (Socket)AccessController.doPrivileged(action);
         }
         catch (PrivilegedActionException e)
         {
            throw (IOException) e.getCause();
         }
      }

      listener.socketCreated(socket, serverSocket);
      return socket;
   }


   public void close() throws IOException
   {
      serverSocket.close();
   }


   public boolean equals(Object obj)
   {
      return serverSocket.equals(obj);
   }


   public ServerSocketChannel getChannel()
   {
      return serverSocket.getChannel();
   }


   public InetAddress getInetAddress()
   {
      return serverSocket.getInetAddress();
   }


   public int getLocalPort()
   {
      return serverSocket.getLocalPort();
   }


   public SocketAddress getLocalSocketAddress()
   {
      return serverSocket.getLocalSocketAddress();
   }


   public int getReceiveBufferSize() throws SocketException
   {
      return serverSocket.getReceiveBufferSize();
   }


   public boolean getReuseAddress() throws SocketException
   {
      return serverSocket.getReuseAddress();
   }


   public int getSoTimeout() throws IOException
   {
      return serverSocket.getSoTimeout();
   }


   public int hashCode()
   {
      return serverSocket.hashCode();
   }


   public boolean isBound()
   {
      return serverSocket.isBound();
   }


   public boolean isClosed()
   {
      return serverSocket.isClosed();
   }


   public void setReceiveBufferSize(int size) throws SocketException
   {
      serverSocket.setReceiveBufferSize(size);
   }


   public void setReuseAddress(boolean on) throws SocketException
   {
      serverSocket.setReuseAddress(on);
   }


   public void setSoTimeout(int timeout) throws SocketException
   {
      serverSocket.setSoTimeout(timeout);
   }


   public String toString()
   {
      return serverSocket.toString();
   }
   
   static private void bind(final ServerSocket ss, final SocketAddress address)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         ss.bind(address);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               ss.bind(address);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   static private void bind(final ServerSocket ss, final SocketAddress address,
                           final int backlog) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         ss.bind(address, backlog);
         return;
      }
      
      try
      {
          AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                ss.bind(address, backlog);
                return null;
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (IOException) e.getCause();
      }
   }
}
