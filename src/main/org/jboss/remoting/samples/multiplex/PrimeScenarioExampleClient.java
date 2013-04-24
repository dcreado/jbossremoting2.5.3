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

/* Created on Jul 24, 2005
 *
 */
package org.jboss.remoting.samples.multiplex;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.jboss.remoting.transport.multiplex.VirtualServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;

/**
 * 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 */
public class PrimeScenarioExampleClient
{  
   public void runPrimeScenario()
   {
      try
      {
         // create a VirtualSocket and connect it to MasterServerSocket
         Socket v1 = new VirtualSocket("localhost", 5555);
         
         // do some asynchronous input in a separate thread
         new AsynchronousThread(v1).start();
         
         // do some synchronous communication
         ObjectOutputStream oos = new ObjectOutputStream(v1.getOutputStream());
         ObjectInputStream ois = new ObjectInputStream(v1.getInputStream());
         oos.writeObject(new Integer(3)); 
         Integer i1 = (Integer) ois.readObject();
         System.out.println("synch: " + i1);
         v1.close();
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
      
      AsynchronousThread(Socket virtualSocket)
      {
         this.virtualSocket = virtualSocket;
      }
      
      public void run()
      {
         try
         {
            // create a VirtualServerSocket that shares a port with socket
            ServerSocket serverSocket = new VirtualServerSocket(virtualSocket.getLocalPort());	
            
            // create a VirtualSocket that shares a port with socket
            serverSocket.setSoTimeout(10000);
            Socket v4 = serverSocket.accept();
            ObjectInputStream ois = new ObjectInputStream(v4.getInputStream());
            
            // get an object from the server 
            v4.setSoTimeout(10000);
            Object o = ois.readObject();
            System.out.println("asynch: " + ((Integer) o));
            serverSocket.close();
            v4.close();
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
      new PrimeScenarioExampleClient().runPrimeScenario();
   }

}


