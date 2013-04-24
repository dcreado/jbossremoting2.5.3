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

//import org.jboss.remoting.transport.socket.OptimizedObjectInputStream;
//import org.jboss.remoting.transport.socket.OptimizedObjectOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RawServer implements Runnable
{
   protected int serverBindPort = 6700;
   protected int backlog = 200;
   protected ServerSocket serverSocket;
   protected InetAddress bindAddress;
   protected String serverBindAddress = "localhost";
   protected int timeout = 60000; // 60 seconds.

   public RawServer()
   {
      try
      {
         bindAddress = InetAddress.getByName(serverBindAddress);
         serverSocket = new ServerSocket(serverBindPort, backlog, bindAddress);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
   }

   public void startServer()
   {
      new Thread(this).start();
   }

   /**
    * When an object implementing interface <code>Runnable</code> is used
    * to create a thread, starting the thread causes the object's
    * <code>run</code> method to be called in that separately executing
    * thread.
    * <p/>
    * The general contract of the method <code>run</code> is that it may
    * take any action whatsoever.
    *
    * @see Thread#run()
    */
   public void run()
   {
      try
      {
         Socket socket = serverSocket.accept();
         socket.setSoTimeout(timeout);

         BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
//         OptimizedObjectOutputStream out = new OptimizedObjectOutputStream(bos);
//         out.flush();
         BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
//         OptimizedObjectInputStream in = new OptimizedObjectInputStream(bis);

         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.flush();
         ObjectInputStream objInputStream = new ObjectInputStream(bis);


         processRequest(objInputStream, oos);

         while(true)
         {
            acknowledge(objInputStream, oos);
            processRequest(objInputStream, oos);
         }


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

   private void processRequest(ObjectInputStream objInputStream, ObjectOutputStream oos)
         throws IOException, ClassNotFoundException
   {
      Object obj = objInputStream.readObject();
      objInputStream.readObject();

//         Object obj = in.readObject();
//         in.readObject(); // for stupid ObjectInputStream reset

      System.out.println("Object read: " + obj);

      Object resp = null;
      try
      {
         // Make absolutely sure thread interrupted is cleared.
         boolean interrupted = Thread.interrupted();
         // call transport on the subclass, get the result to handback
         resp = "This is response.";
      }
      catch(Exception ex)
      {
         resp = ex;
      }

      Thread.interrupted(); // clear interrupted state so we don't fail on socket writes


      oos.writeObject(resp);

      //oos.flush();

      oos.reset();
      // to make sure stream gets reset
      // Stupid ObjectInputStream holds object graph
      // can only be set by the client/server sending a TC_RESET
      oos.writeObject(Boolean.TRUE);
      oos.flush();
      oos.reset();
   }

   private void acknowledge(ObjectInputStream objInputStream, ObjectOutputStream oos) throws IOException
   {
      // now stay open and wait for ack
      System.out.println("waiting for ack");
      byte ACK = objInputStream.readByte();
      System.out.println("got ack");
      oos.writeByte(ACK);
      oos.flush();
      oos.reset();

   }

   public static void main(String[] args)
   {
      RawServer server = new RawServer();
      server.startServer();
   }
}