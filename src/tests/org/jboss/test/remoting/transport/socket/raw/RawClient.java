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

package org.jboss.test.remoting.transport.socket.raw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RawClient
{
   protected String address = "localhost";
   protected int port = 6700;

   public boolean enableTcpNoDelay = false;
   public int timeout = 60000;

   private Socket socket = null;

   private ObjectOutputStream oos;
   private ObjectInputStream objInputStream;

   public void startClient()
   {
      while(true)
      {
//         try
//         {
//            Thread.sleep(1000);
//         }
//         catch(InterruptedException e)
//         {
//            e.printStackTrace();
//         }

         try
         {
            getSocket();

            oos.writeObject("This is the request");

            //oos.flush();

            oos.reset();
            // to make sure stream gets reset
            // Stupid ObjectInputStream holds object graph
            // can only be set by the client/server sending a TC_RESET
            oos.writeObject(Boolean.TRUE);
            oos.flush();
            oos.reset();


            Object obj = objInputStream.readObject();

            objInputStream.readObject(); // for stupid ObjectInputStream reset

            System.out.println("response: " + obj);

         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
         catch(ClassNotFoundException e)
         {
            e.printStackTrace();
         }
      }
   }

   public void getSocket() throws IOException
   {
      if(socket == null)
      {
         try
         {
            socket = new Socket(address, port);
            socket.setTcpNoDelay(enableTcpNoDelay);
//            socket.setSoTimeout(timeout);

            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
//         out.flush();
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            oos = new ObjectOutputStream(out);
            objInputStream = new ObjectInputStream(in);

         }
         catch(IOException e)
         {
            e.printStackTrace();
         }
      }
      else
      {
         oos.reset();
         oos.writeByte(1);
         oos.flush();
         oos.reset();
         objInputStream.readByte();
//         objInputStream.reset();
      }
   }

   public static void main(String[] args)
   {
      RawClient client = new RawClient();
      client.startClient();
   }
}