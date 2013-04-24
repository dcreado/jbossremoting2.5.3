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
 * Created on Aug 21, 2005
 */
 
package org.jboss.remoting.transport.multiplex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.jboss.logging.Logger;


/**
 * <code>VirtualServerSocketFactory</code> extends the
 * <code>javax.net.ServerSocketFactory</code> class and reimplements
 * its <code>createServerSocket()</code> methods.  By default it creates
 * <code>MasterServerSocket</code>s, but
 * if the <code>setOnClient()</code> method has been called and the
 * <code>setOnServer()</code> method has not
 * been called more recently, <code>VirtualServerSocketFactory</code> creates
 * <code>VirtualServerSocket</code>s.
 * 
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public class VirtualServerSocketFactory extends ServerSocketFactory
{
   protected static final Logger log = Logger.getLogger(VirtualServerSocketFactory.class);
   boolean onClient = false;

   public static ServerSocketFactory getDefault()
   {
      return new VirtualServerSocketFactory();
   }
   
   
   protected VirtualServerSocketFactory()
   {
   }


//////////////////////////////////////////////////////////////////////////////////////////////////
///             The following methods are required of all ServerSocketFactory's               '///
//////////////////////////////////////////////////////////////////////////////////////////////////
   
/**
 * See superclass javadoc.
 */
   public ServerSocket createServerSocket(int port) throws IOException
   {
      if (isOnClient())
         return new VirtualServerSocket(port);
      else
         return new MasterServerSocket(port);
   }

   
/**
 * See superclass javadoc.
 */
   public ServerSocket createServerSocket(int port, int backlog) throws IOException
   {
      if (isOnClient())
      {
         log.warn("backlog parameter is ignored");
         return new VirtualServerSocket(port, backlog);
      }
      else
         return new MasterServerSocket(port, backlog);
   }

   
/**
 * See superclass javadoc.
 */
   public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException
   {
      if (isOnClient())
      {
         log.warn("backlog parameter is ignored");
         return new VirtualServerSocket(port, backlog, address);
      }
      else
         return new MasterServerSocket(port, backlog, address);
   }
   
   

//////////////////////////////////////////////////////////////////////////////////////////////////
///             The following methods are specific to VirtualServerSocketFactory              '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * @return
 */
   public boolean isOnClient()
   {
      return onClient;
   }
   
   
/**
 * @return
 */
   public boolean isOnServer()
   {
      return !onClient;
   }
   
   
/**
 * @return
 */
   public boolean setOnClient()
   {
      boolean temp = onClient;
      onClient = true;
      return temp;
   }
   
   
/**
 * @return
 */
   public boolean setOnServer()
   {
      boolean temp = onClient;
      onClient = false;
      return !temp;
   }
}

