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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.net.ServerSocketFactory;

import org.jboss.remoting.util.SecurityUtility;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerSocketFactoryWrapper extends ServerSocketFactory
{
   private ServerSocketFactoryMBean serverSocketFactory = null;

   public ServerSocketFactoryWrapper(ServerSocketFactoryMBean serverSocketFactory)
   {
      this.serverSocketFactory = serverSocketFactory;
   }

   public ServerSocket createServerSocket(final int i) throws IOException
   {
      return createServerSocket(serverSocketFactory, i);
   }

   public ServerSocket createServerSocket(final int i, final int i1) throws IOException
   {
      return createServerSocket(serverSocketFactory, i, i1);
   }

   public ServerSocket createServerSocket(final int i, final int i1, final InetAddress inetAddress) throws IOException
   {
      return createServerSocket(serverSocketFactory, i, i1, inetAddress);
   }

   public ServerSocket createServerSocket() throws IOException
   {
      return createServerSocket(serverSocketFactory);
   }

   public ServerSocketFactoryMBean getDelegate()
   {
      return serverSocketFactory;
   }
   
   static private ServerSocket createServerSocket(final ServerSocketFactoryMBean ssf) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ssf.createServerSocket();
      }

      try
      {
         return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return ssf.createServerSocket();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private ServerSocket createServerSocket(final ServerSocketFactoryMBean ssf,
                                                 final int port) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ssf.createServerSocket(port);
      }
      
      try
      {
          return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                 return ssf.createServerSocket(port);
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (IOException) e.getCause();
      }
   }
   
   static private ServerSocket createServerSocket(final ServerSocketFactoryMBean ssf,
                                                 final int port, final int backlog)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ssf.createServerSocket(port, backlog);
      }

      try
      {
         return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return ssf.createServerSocket(port, backlog);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private ServerSocket createServerSocket(final ServerSocketFactoryMBean ssf,
                                                 final int port, final int backlog,
                                                 final InetAddress inetAddress)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return ssf.createServerSocket(port, backlog, inetAddress);
      }

      try
      {
         return (ServerSocket)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return ssf.createServerSocket(port, backlog, inetAddress);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
}