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
package org.jboss.test.remoting.socketfactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.SocketFactory;

import org.apache.log4j.Logger;
import org.jboss.remoting.socketfactory.SocketCreationListener;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1885 $
 * <p>
 * Copyright Jan 16, 2007
 * </p>
 */
public class TestListener implements SocketCreationListener
{
   private static Logger log = Logger.getLogger(TestListener.class);
   private boolean visited;
   private boolean client;
   
   public void socketCreated(Socket socket, Object source) throws IOException
   {
      visited = true;
      if (source instanceof SocketFactory)
         client = true;
      else if (source instanceof ServerSocket)
         client = false;
      else
         log.error("unrecognized socket source: " + source);
      log.info(this + ": " + source);
   }
   
   public boolean visited()
   {
      return visited;
   }
   
   public boolean isClient()
   {
      return client;
   }
   
   public void reset()
   {
      visited = false;
   }
}
