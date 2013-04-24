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
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.MasterServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;

/**
 * A ManyToOneServer.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 590 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class N_SocketScenarioServer
{
   static int bindPort = 6666;
   static String connectHost = "localhost";
   static int connectPort = 5555;
   
   public void runN_SocketScenario()
   {
      try
      {
         // Create and bind a MasterServerSocket.
         MasterServerSocket serverSocket = new MasterServerSocket(bindPort);
         
         // Accept connection request from  VirtualServerSocket.
         serverSocket.setSoTimeout(10000);
         serverSocket.acceptServerSocketConnection();
         
         // Create 3 virtual sockets
         Thread.sleep(2000);
         Socket socket1 = new VirtualSocket(connectHost, connectPort);
         Socket socket2 = new VirtualSocket(connectHost, connectPort);
         Socket socket3 = new VirtualSocket(connectHost, connectPort);
         
         // Do some i/o.
         InputStream is1 = socket1.getInputStream();
         OutputStream os1 = socket1.getOutputStream();
         InputStream is2 = socket2.getInputStream();
         OutputStream os2 = socket2.getOutputStream();
         InputStream is3 = socket3.getInputStream();
         OutputStream os3 = socket3.getOutputStream();
         os1.write(is1.read());
         os2.write(is2.read());
         os3.write(is3.read());
         
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
      
      new N_SocketScenarioServer().runN_SocketScenario();
   }
}

