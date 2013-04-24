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
 * Created on Sep 12, 2005
 */

package org.jboss.remoting.samples.multiplex;

import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.remoting.transport.multiplex.VirtualServerSocketFactory;
import org.jboss.remoting.transport.multiplex.VirtualSocketFactory;


/**
 * A FactoryExample.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 590 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class FactoryExample
{
   
   void runFactoryExample()
   {
      ServerSocketFactory serverSocketFactory = VirtualServerSocketFactory.getDefault();
      ((VirtualServerSocketFactory) serverSocketFactory).setOnServer();
      SocketFactory socketFactory = VirtualSocketFactory.getDefault();
      useServerSocketFactory(serverSocketFactory);
      useSocketFactory(socketFactory);
   }
   
   void useServerSocketFactory(final ServerSocketFactory serverSocketFactory)
   {
      new Thread()
      {
         public void run()
         {
            try
            {
               ServerSocket serverSocket = serverSocketFactory.createServerSocket(5555);
               Socket socket = serverSocket.accept();
               int b = socket.getInputStream().read();
               socket.getOutputStream().write(b);
               socket.close();
               serverSocket.close();
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      }.start();
   }
   
   public void useSocketFactory(SocketFactory socketFactory)
   {
      try
      {
         Thread.sleep(1000);
         Socket socket = socketFactory.createSocket("localhost", 5555);
         socket.getOutputStream().write(7);
         System.out.println(socket.getInputStream().read());
         socket.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      
   }

   public static void main(String[] args)
   {
      new FactoryExample().runFactoryExample();
   }
}

