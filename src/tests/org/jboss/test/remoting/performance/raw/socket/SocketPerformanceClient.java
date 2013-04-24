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
import junit.framework.Test;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.performance.synchronous.PerformanceCallbackKeeper;
import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.server.UID;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketPerformanceClient extends PerformanceClientTest
{

   private int port = 7600;
   private Socket socket = null;
   private ObjectOutputStream oos;
   private ObjectInputStream objInputStream;
   private int timeout = 20000;

   private String clientSessionId = new UID().toString();

   protected static final Logger log = Logger.getLogger(SocketPerformanceClient.class);

   public static Test suite()
   {
      return new ThreadLocalDecorator(SocketPerformanceClient.class, 1);
   }

   public void init()
   {
//      try
//      {
//         getSocket();
//      }
//      catch(IOException e)
//      {
//         e.printStackTrace();
//      }
   }

   protected InvokerLocator initServer(int port) throws Exception
   {
      return null;
   }

   protected PerformanceCallbackKeeper addCallbackListener(String sessionId, Latch serverDoneLock)
         throws Throwable
   {
      SocketCallbackServer callbackServer = new SocketCallbackServer(host, clientSessionId, serverDoneLock);
      callbackServer.start();
      makeInvocation("callbackserver", new Integer(callbackServer.getBindPort()));
      return callbackServer;
   }

   protected void populateMetadata(Map metadata)
   {
      super.populateMetadata(metadata);
      metadata.put("transport", "raw_socket");
      metadata.put("serialization", "java");
   }

   protected Object getBenchmarkAlias()
   {
      String config = System.getProperty("alias");
      if(config == null || config.length() == 0)
      {
         config = System.getProperty("jboss-junit-configuration");
         if(config == null || config.length() == 0)
         {
            config = "raw_socket" + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + "java";
         }
      }
      return config;
   }


   protected Object makeInvocation(String method, Object param) throws Throwable
   {

//      Socket socket = new Socket(address, port);
//      socket.setSoTimeout(timeout);
//      BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
//      BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//
//      ObjectOutputStream oos = new ObjectOutputStream(out);
//      ObjectInputStream objInputStream = new ObjectInputStream(in);

      getSocket();

      SocketPayload payload = new SocketPayload(method, clientSessionId, param);

      oos.writeObject(payload);
      oos.reset();
      oos.writeObject(Boolean.TRUE);
      oos.flush();
      oos.reset();

      Object obj = objInputStream.readObject();
      objInputStream.readObject();

//      objInputStream.close();
//      oos.close();
//      socket.close();


      return obj;
   }

   private void getSocket() throws IOException
   {
      if(socket == null)
      {
         try
         {
            socket = new Socket(host, port);
            socket.setSoTimeout(timeout);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            oos = new ObjectOutputStream(out);
            objInputStream = new ObjectInputStream(in);
         }
         catch(IOException e)
         {
            e.printStackTrace();
            throw e;
         }
      }
      else
      {
         oos.reset();
         oos.writeByte(1);
         oos.flush();
         oos.reset();
         objInputStream.readByte();
      }
   }

   public static void main(String[] args)
   {
      SocketPerformanceClient test = new SocketPerformanceClient();
      try
      {
         test.setUp();
         test.testClientCalls();
         test.tearDown();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }


}