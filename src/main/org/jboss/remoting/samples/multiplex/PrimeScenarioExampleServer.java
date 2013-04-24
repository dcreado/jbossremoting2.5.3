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
 * Created on Jul 24, 2005
 */
package org.jboss.remoting.samples.multiplex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.MasterServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;

/**
 * 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 */
public class PrimeScenarioExampleServer
{
   public void runPrimeScenario()
   {
      try
      {
         // create a MasterServerSocket and get a VirtualSocket
         ServerSocket serverSocket = new MasterServerSocket(5555);
         serverSocket.setSoTimeout(10000);
         Socket v2 = serverSocket.accept();
         
         // do some asynchronous communication in a separate thread
         Thread asynchronousThread = new AsynchronousThread(v2);
         asynchronousThread.start();
         
         // do some synchronous communication
         ObjectInputStream ois = new ObjectInputStream(v2.getInputStream());
         ObjectOutputStream oos = new ObjectOutputStream(v2.getOutputStream());
         v2.setSoTimeout(10000);
         Object o = ois.readObject();
         oos.writeObject(o);
         
         asynchronousThread.join();
         serverSocket.close();
         v2.close();
      }
      
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(1);
      }
   }
   
   
   class AsynchronousThread extends Thread
   {
      private Socket virtualSocket;
      
      public AsynchronousThread(Socket socket) throws IOException
      {
         this.virtualSocket = socket;
      }
      
      public void run()
      {   
         try
         {
            // Give VirtualServerSocket a chance to start.
            Thread.sleep(2000);
            
            // connect to VirtualServerSocket
            String hostName = virtualSocket.getInetAddress().getHostName();
            int port = virtualSocket.getPort();
            Socket v3 = new VirtualSocket(hostName, port);
            
            // send an object to the client
            ObjectOutputStream oos = new ObjectOutputStream(v3.getOutputStream());
            oos.writeObject(new Integer(7));
            
            oos.flush();
            v3.close();
         }
         catch (Exception e)
         {
            e.printStackTrace();
            System.exit(1);
         }
      }
   }
   
   
   
   public static void main(String[] args)
   {
      new PrimeScenarioExampleServer().runPrimeScenario();
   }
   

}
