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
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/** 
 * A CreationListenerSocketFactory wraps a SocketFactory and notifies a listener
 * of the creation of a Socket before returning the socket.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1866 $
 * <p>
 * Copyright Jan 10, 2007
 * </p>
 */
public class CreationListenerSocketFactory
   extends SSLSocketFactory
   implements SocketFactoryWrapper, Serializable
{
   private static final long serialVersionUID = 1210774093889434553L;
   
   private SocketFactory factory;
   private SocketCreationListener listener;
   
   
   public CreationListenerSocketFactory(SocketCreationListener listener)
   {
      this(SocketFactory.getDefault(), listener);
   }
   
   
   public CreationListenerSocketFactory(SocketFactory factory, SocketCreationListener listener)
   {
      this.factory = factory;
      this.listener = listener;
   }
   
   public SocketFactory getFactory()
   {
      return factory;
   }
   
   
   public SocketCreationListener getListener()
   {
      return listener;
   }
   
   
   public void setFactory(SocketFactory factory)
   {
      this.factory = factory;
   }
   
  
   public void setListener(SocketCreationListener listener)
   {
      this.listener = listener;
   }
   
   
   public Socket createSocket() throws IOException
   {
      checkFactory();
      Socket socket = factory.createSocket();
      listener.socketCreated(socket, factory);
      return socket;
   }
   
   
   public Socket createSocket(String host, int port)
   throws IOException, UnknownHostException
   {
      checkFactory();
      Socket socket = factory.createSocket(host, port);
      listener.socketCreated(socket, factory);
      return socket;
   }
   

   public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
   throws IOException, UnknownHostException
   {
      checkFactory();
      Socket socket = factory.createSocket(host, port, clientHost, clientPort);
      listener.socketCreated(socket, factory);
      return socket;
   }

   
   public Socket createSocket(InetAddress host, int port) throws IOException
   {
      checkFactory();
      Socket socket = factory.createSocket(host, port);
      listener.socketCreated(socket, factory);
      return socket;
   }

   
   public Socket createSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort)
   throws IOException
   {
      checkFactory();
      Socket socket = factory.createSocket(address, port, clientAddress, clientPort);
      listener.socketCreated(socket, factory);
      return socket;
   }


   public String[] getDefaultCipherSuites()
   {
      if (factory instanceof SSLSocketFactory)
         return ((SSLSocketFactory) factory).getDefaultCipherSuites();
      else
         return null;
   }


   public String[] getSupportedCipherSuites()
   {
      if (factory instanceof SSLSocketFactory)
         return ((SSLSocketFactory) factory).getSupportedCipherSuites();
      else
         return null;
   }


   public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
   {
      if (factory instanceof SSLSocketFactory)
      {
         Socket socket = ((SSLSocketFactory) factory).createSocket(s, host, port, autoClose);
         listener.socketCreated(socket, factory);
         return socket;
      }
      else
         return null;
   }
   
  
   public SocketFactory getSocketFactory()
   {
      return factory;
   }


   public void setSocketFactory(SocketFactory factory)
   {
      this.factory = factory;
   }
   
   
   protected void checkFactory()
   {
      if (factory == null)
         factory = SocketFactory.getDefault();
   }
}
