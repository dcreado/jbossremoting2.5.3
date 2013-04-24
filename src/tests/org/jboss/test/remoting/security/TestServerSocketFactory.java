
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
package org.jboss.test.remoting.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.apache.log4j.Logger;
import org.jboss.remoting.security.SSLServerSocketFactoryService;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 7, 2008
 * </p>
 */
public class TestServerSocketFactory
   extends SSLServerSocketFactoryService
   implements TestServerSocketFactoryMBean
{
   private static Logger log = Logger.getLogger(TestServerSocketFactory.class);
   
   int counter;
   
   public ServerSocket createServerSocket()
   throws IOException
   {
      counter++;
      log.info("createServerSocket()");
      return super.createServerSocket();
   }

   public ServerSocket createServerSocket( int port )
   throws IOException
   {
      counter++;
      log.info("createServerSocket(port)");
      return super.createServerSocket(port);
   }

   public ServerSocket createServerSocket( int port,
                                           int backlog )
   throws IOException
   {
      counter++;
      log.info("createServerSocket(port, backlog)");
      return super.createServerSocket(port, backlog);
   }

   public ServerSocket createServerSocket( int         port,
                                           int         backlog,
                                           InetAddress ifAddress )
   throws IOException
   {
      counter++;
      log.info("createServerSocket(port, backlog, ifAddress)");
      return super.createServerSocket(port, backlog, ifAddress);
   }

   public int getCounter()
   {
      return counter;
   }
}

