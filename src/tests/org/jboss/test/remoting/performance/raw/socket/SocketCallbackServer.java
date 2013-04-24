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

package org.jboss.test.remoting.performance.raw.socket;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;

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
public class SocketCallbackServer implements PerformanceCallbackKeeper
{
   private String serverBindAddress = "localhost";
   private int serverBindPort = 6710;
   private InetAddress bindAddress;
   private ServerSocket serverSocket;
   private int backlog = 2;

   private String sessionId;
   private Latch lock;
   private int numberOfCallsProcessed = 0;
   private int numberofDuplicates = 0;

   public SocketCallbackServer(String host, String sessionId, Latch lock)
   {
      this.serverBindAddress = host;
      this.sessionId = sessionId;
      this.lock = lock;
   }

   public int getNumberOfCallsProcessed()
   {
      return numberOfCallsProcessed;
   }

   public int getNumberOfDuplicates()
   {
      return numberofDuplicates;
   }

   public int getBindPort()
   {
      return serverBindPort;
   }

   public void start() throws Exception
   {
      calculateBindPort();

      bindAddress = InetAddress.getByName(serverBindAddress);
      serverSocket = new ServerSocket(serverBindPort, backlog, bindAddress);
      System.out.println("started SocketCallbackServer on port " + serverBindPort);
      // this was done inline since TestCase already has a void parameter run() method
      // so could not create a run() method for the Runnable implementation.
      for(int x = 0; x < 1; x++)
      {
         new Thread()
         {
            public void run()
            {
               try
               {
                  startServer();
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
            }
         }.start();
      }

   }

   private void calculateBindPort()
   {
      serverBindPort = TestUtil.getRandomPort();
   }

   private void startServer()
   {
      try
      {
         Socket socket = serverSocket.accept();

         BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
         BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.flush();
         ObjectInputStream objInputStream = new ObjectInputStream(bis);

         processRequest(objInputStream, oos);

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }


   private void processRequest(ObjectInputStream objInputStream, ObjectOutputStream oos)
         throws IOException, ClassNotFoundException, HandleCallbackException
   {
      System.out.println("SocketCallbackServer::procesRequest() called " + serverBindPort);
      Object obj = objInputStream.readObject();
      try
      {
         objInputStream.readObject();

         oos.writeObject(Boolean.TRUE);
         oos.reset();
         oos.writeObject(Boolean.TRUE);
         oos.flush();
         oos.reset();
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
      catch(ClassNotFoundException e)
      {
         e.printStackTrace();
      }

      Callback callback = (Callback) obj;
      handleCallback(callback);

   }


   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      System.out.println("SocketCallbackServer::handleCallback() called " + serverBindPort);
      Object ret = callback.getCallbackObject();
      Integer[] handledArray = (Integer[]) ret;
      Integer numOfCallsHandled = (Integer) handledArray[0];
      Integer numOfDuplicates = (Integer) handledArray[1];
      System.out.println("Server is done.  Number of calls handled: " + numOfCallsHandled);
      numberOfCallsProcessed = numOfCallsHandled.intValue();
      System.out.println("Number of duplicate calls: " + numOfDuplicates);
      numberofDuplicates = numOfDuplicates.intValue();
      Object obj = callback.getCallbackHandleObject();
      //String handbackObj = (String) obj;
      //System.out.println("Handback object should be " + sessionId + " and server called back with " + handbackObj);
      lock.release();
      System.out.println("SocketCallbackServer - released lock " + serverBindPort);
   }


}