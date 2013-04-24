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

package org.jboss.test.remoting.serialization;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.serialization.SerializationStreamFactory;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class FactoryServer extends ServerTestCase
{
   private int serverBindPort = 6700;
   private int backlog = 200;
   private ServerSocket serverSocket;
   private InetAddress bindAddress;
   private String serverBindAddress = "localhost";
   private int timeout = 60000; // 60 seconds.

   public static final String RESPONSE = "This is the response";

   private boolean keepRunning = true;

   public void setUp() throws Exception
   {
      bindAddress = InetAddress.getByName(serverBindAddress);
      serverSocket = new ServerSocket(serverBindPort, backlog, bindAddress);
      new Thread(new ServerThread()).start();
   }

   public void tearDown() throws Exception
   {
      keepRunning = false;
      serverSocket.close();
   }

   private class ServerThread implements Runnable
   {


      public void run()
      {
         try
         {
            configureStream();

            while(keepRunning)
            {
               try
               {
                  Socket socket = serverSocket.accept();
                  socket.setSoTimeout(timeout);

                  try
                  {
                     ObjectOutput output = SerializationStreamFactory.getManagerInstance().createOutput(socket.getOutputStream());
                     ObjectInput input = SerializationStreamFactory.getManagerInstance().createRegularInput(socket.getInputStream());

                     processRequest(input, output);
                  }
                  catch(Exception e)
                  {
                     e.printStackTrace();
                  }

               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
            }
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }

      private void processRequest(ObjectInput objInputStream, ObjectOutput oos)
            throws IOException, ClassNotFoundException
      {
         Object obj = objInputStream.readObject();
         System.out.println("Object read: " + obj);

         oos.writeObject(RESPONSE);
         oos.flush();
      }
   }

   protected abstract void configureStream() throws Exception;

}