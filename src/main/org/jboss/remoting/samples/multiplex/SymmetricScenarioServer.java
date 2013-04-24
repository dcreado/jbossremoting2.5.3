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
 * Created on Sep 9, 2005
 */
package org.jboss.remoting.samples.multiplex;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.MasterServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;


/**
 * A SymmetricScenarioServer.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 590 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class SymmetricScenarioServer
{
   static int bindPort = 6666;
   static String connectHost = "localhost";
   static int connectPort = 5555;

   
   public void runSymmetricScenario()
   {
      try
      {
         // Create ServerSocket and get synchronizing socket.
         ServerSocket ss = new ServerSocket(bindPort);
         Socket syncSocket = ss.accept();
         ss.close();
         InputStream is_sync = syncSocket.getInputStream();
         OutputStream os_sync = syncSocket.getOutputStream();

         // Create MasterServerSocket, accept connection request from remote
         // VirtualServerSocket, and get the bind port of the local actual socket
         // to which the VirtualServerSocket is connected. 
         MasterServerSocket mss = new MasterServerSocket(bindPort + 1);
         os_sync.write(3);
         mss.setSoTimeout(5000);
         int port = mss.acceptServerSocketConnection();
         mss.close();
                
         // Wait until remote VirtualServerSocket is running, then create local
         // VirtualServerSocket, bind it to the local port to which the remote
         // VirtualServerSocket is connected, and connect it to the remote
         // VirtualServerSocket.
         is_sync.read();
         VirtualServerSocket vss = new VirtualServerSocket(port);
         InetSocketAddress address = new InetSocketAddress(connectHost, connectPort);
         vss.setSoTimeout(5000);
         vss.connect(address);
         
         // Indicate that the local VirtualServerSocket is running.
         os_sync.write(7);
         
         // Create a virual socket by way of VirtualServerSocket.accept();
         Socket virtualSocket1 = vss.accept();
         InputStream is1 = virtualSocket1.getInputStream();
         OutputStream os1 = virtualSocket1.getOutputStream();
         
         // Create a virtual socket connected to the remote VirtualServerSocket.
         Socket virtualSocket2 = new VirtualSocket(connectHost, connectPort);
         InputStream is2 = virtualSocket2.getInputStream();
         OutputStream os2 = virtualSocket2.getOutputStream();
         
         // Do some i/o and close sockets.
         os1.write(is1.read());
         os2.write(is2.read());
         virtualSocket1.close();
         virtualSocket2.close();
         syncSocket.close();
         vss.close();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


   public static void main(String[] args)
   {
      if (args.length == 3)
      {
         bindPort = Integer.parseInt(args[0]);
         connectHost = args[1];
         connectPort = Integer.parseInt(args[2]);
      }
      
      new SymmetricScenarioServer().runSymmetricScenario();
   }
}

