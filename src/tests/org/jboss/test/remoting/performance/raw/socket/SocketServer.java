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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import org.jboss.test.remoting.performance.synchronous.CallTracker;
import org.jboss.test.remoting.performance.synchronous.Payload;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketServer
{
   private int serverBindPort = 7600;
   private int backlog = 200;
   private ServerSocket serverSocket;
   private InetAddress bindAddress;
   private String serverBindAddress = "localhost";
   private int timeout = 20000;

   private int serverReadThreads;

   private ThreadLocal callbackServerPort = new ThreadLocal();

   private boolean continueToRun = true; // flag to keep the server listening

//   private int requestCounter = 0;

   private Map callTrackers = new ConcurrentHashMap();

   private static final Logger log = Logger.getLogger(SocketServer.class);

   public SocketServer(String host, int numOfClients)
   {
      serverBindAddress = host;
      serverReadThreads = numOfClients;
   }

   protected void setUp() throws Exception
   {
      System.out.println("SimpleServerTest::setUp() called.");
//      org.apache.log4j.Category.getRoot().info("SimpleServerTest::setUp() called.");
      bindAddress = InetAddress.getByName(serverBindAddress);
      System.out.println("Starting ServerSocket on " + serverBindPort + ", " + serverBindAddress);
      serverSocket = new ServerSocket(serverBindPort, backlog, bindAddress);

      // this was done inline since TestCase already has a void parameter run() method
      // so could not create a run() method for the Runnable implementation.
      for(int x = 0; x < serverReadThreads; x++)
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

   private void startServer()
   {
      try
      {
         Socket socket = serverSocket.accept();
         socket.setSoTimeout(timeout);
         
         BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
         BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
         
         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.flush();
         ObjectInputStream objInputStream = new ObjectInputStream(bis);
         
         processRequest(objInputStream, oos);
         
         //  objInputStream.close();
         //  oos.close();
         //  socket.close();
         
         while(continueToRun)
         {
            acknowledge(objInputStream, oos);
            processRequest(objInputStream, oos);
         }
      }
      catch (EOFException e)
      {
         log.debug(e);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         //         org.apache.log4j.Category.getRoot().debug("Done processing on client socket.");
      }
      System.out.println("Done processing on client socket.");
   }


   private void processRequest(ObjectInputStream objInputStream, ObjectOutputStream oos)
         throws Exception
   {
      Object obj = objInputStream.readObject();
      objInputStream.readObject();

      SocketPayload payload = (SocketPayload) obj;
      Object response = processPayload(payload);

      Thread.interrupted(); // clear interrupted state so we don't fail on socket writes

      oos.writeObject(response);
      oos.reset();
      oos.writeObject(Boolean.TRUE);
      oos.flush();
      oos.reset();

//      ++requestCounter;
   }

   private Object processPayload(SocketPayload payload) throws Exception
   {
      String method = payload.getMethod();
      String sessionId = payload.getSessionId();
      Object param = payload.getPayload();
      if(PerformanceClientTest.NUM_OF_CALLS.equals(method))
      {
         Integer totalCountInteger = (Integer) param;
         int totalCount = totalCountInteger.intValue();
         System.out.println("received totalCallCount call with total count of " + totalCount + " from " + sessionId);
         CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
         if(tracker != null)
         {
            tracker.createTotalCount(totalCount);
         }
         else
         {
            int port = ((Integer)callbackServerPort.get()).intValue();
            SocketHandler callbackHandler = new SocketHandler(serverBindAddress, sessionId, port);
            tracker = new CallTracker(sessionId, callbackHandler);
            callTrackers.put(sessionId, tracker);
            tracker.createTotalCount(totalCount);
         }
         return totalCountInteger;

      }
      else if(PerformanceClientTest.TEST_INVOCATION.equals(method))
      {
         Payload clientPayload = (Payload) param;
         int clientInvokerCallCount = clientPayload.getCallNumber();

         CallTracker tracker = (CallTracker) callTrackers.get(sessionId);
         if(tracker != null)
         {
            tracker.verifyClientInvokeCount(clientInvokerCallCount);
         }
         else
         {
            log.error("No call tracker exists for session id " + sessionId);
            throw new Exception("No call tracker exists for session id " + sessionId);
         }
         return new Integer(clientInvokerCallCount);
      }
      else if("callbackserver".equals(method))
      {
//         callbackServerPort = ((Integer) param).intValue();
         callbackServerPort.set(param);
         return null;
      }
      else
      {
         throw new Exception("Can not process for method " + method);
      }
   }

   private void acknowledge(ObjectInputStream objInputStream, ObjectOutputStream oos) throws IOException
   {
      byte ACK = objInputStream.readByte();
      oos.writeByte(ACK);
      oos.flush();
      oos.reset();
   }


   public void testRequestCount()
   {
      while(continueToRun)
      {
         try
         {
            Thread.currentThread().sleep(10000);
         }
         catch(InterruptedException e)
         {
            e.printStackTrace();
         }
//         System.out.println("Requests taken: " + requestCounter);
//         org.apache.log4j.Category.getRoot().info("SimpleServerTest::testRequestCount() - Requests taken: " + requestCounter);
      }

//      org.apache.log4j.Category.getRoot().info("SimpleServerTest::testRequestCount() - Total request taken: " + requestCounter);
   }

   protected void tearDown() throws Exception
   {
      continueToRun = false;

//      System.out.println("Tearing down.  Processed " + requestCounter + " requests");
//      org.apache.log4j.Category.getRoot().info("Tearing down.  Processed " + requestCounter + " requests");

      if(serverSocket != null && !serverSocket.isClosed())
      {
         serverSocket.close();
         serverSocket = null;
      }
   }

   public static void main(String[] args)
   {
      int numOfClients = 1;
      String numOfClientsString = System.getProperty(PerformanceTestCase.NUMBER_OF_CLIENTS);
      if(numOfClientsString != null && numOfClientsString.length() > 0)
      {
         try
         {
            numOfClients = Integer.parseInt(numOfClientsString);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      } 
      
      String serverBindAddress = "localhost";
      SocketServer server = new SocketServer(serverBindAddress, numOfClients);
      try
      {
         server.setUp();
         server.testRequestCount();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}