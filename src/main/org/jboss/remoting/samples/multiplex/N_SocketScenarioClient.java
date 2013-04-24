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
 * Created on Sep 8, 2005
 */
 
package org.jboss.remoting.samples.multiplex;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.VirtualServerSocket;


/**
 * A ManyToOneClient.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 590 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class N_SocketScenarioClient
{
   static int bindPort = 5555;
   static String connectHost = "localhost";
   static int connectPort = 6666;
   
   public void runN_SocketScenario()
   {
      try
      {
         // Create a VirtualServerSocket and  connect it to the server.
         VirtualServerSocket serverSocket = new VirtualServerSocket(bindPort);
         InetSocketAddress connectAddress = new InetSocketAddress(connectHost, connectPort);
         serverSocket.setSoTimeout(10000);
         serverSocket.connect(connectAddress);
         
         // Accept connection requests for 3 virtual sockets.
         Socket socket1 = serverSocket.accept();
         Socket socket2 = serverSocket.accept();
         Socket socket3 = serverSocket.accept();
         
         // Do some i/o.
         InputStream is1 = socket1.getInputStream();
         OutputStream os1 = socket1.getOutputStream();
         InputStream is2 = socket2.getInputStream();
         OutputStream os2 = socket2.getOutputStream();
         InputStream is3 = socket3.getInputStream();
         OutputStream os3 = socket3.getOutputStream();
         os1.write(3);
         os2.write(7);
         os3.write(11);
         System.out.println(is1.read());
         System.out.println(is2.read());
         System.out.println(is3.read());
         
         socket1.close();
         socket2.close();
         socket3.close();
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
      
      new N_SocketScenarioClient().runN_SocketScenario();
   }
}

