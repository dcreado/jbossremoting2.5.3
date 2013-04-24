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
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.VirtualServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;


/**
 * A SymmetricScenarioClient.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 590 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class SymmetricScenarioClient
{
   static int bindPort = 5555;
   static String connectHost = "localhost";
   static int connectPort = 6666;
   

   public void runSymmetricScenario()
   {
      try
      {
         // Get a virtual socket to use for synchronizing client and server.
         Socket syncSocket = new Socket(connectHost, connectPort);
         InputStream is_sync = syncSocket.getInputStream();
         OutputStream os_sync = syncSocket.getOutputStream();
         
         // Create a VirtualServerSocket and connect it to MasterServerSocket
         // running on the server.
         VirtualServerSocket serverSocket = new VirtualServerSocket(bindPort);
         InetSocketAddress address1 = new InetSocketAddress(connectHost, connectPort + 1);
         is_sync.read();
         serverSocket.setSoTimeout(5000);
         serverSocket.connect(address1);
         
         // Call constructor to create a virtual socket and connect it to the port on the server
         // to which the VirtualServerSocket is connected.
         os_sync.write(5);
         is_sync.read();
         int port = serverSocket.getRemotePort();
         Socket virtualSocket1 = new VirtualSocket(connectHost, port);
         InputStream is1 = virtualSocket1.getInputStream();
         OutputStream os1 = virtualSocket1.getOutputStream();
         
         // Create a virtual socket by way of VirtualServerSocket.accept().
         Socket virtualSocket2 = serverSocket.accept();
         InputStream is2 = virtualSocket2.getInputStream();
         OutputStream os2 = virtualSocket2.getOutputStream();
         
         // Do some i/o and close sockets.
         os1.write(9);
         System.out.println(is1.read());
         os2.write(11);
         System.out.println(is2.read());
         virtualSocket1.close();
         virtualSocket2.close();
         syncSocket.close();
         serverSocket.close();
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
      
      new SymmetricScenarioClient().runSymmetricScenario();
   }
}

