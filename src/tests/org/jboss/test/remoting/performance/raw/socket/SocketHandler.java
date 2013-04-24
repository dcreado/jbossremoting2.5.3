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

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketHandler implements InvokerCallbackHandler
{
   private String serverBindAddress = "localhost";
   private int serverBindPort = 6710;

   private String sessionId;

   public SocketHandler(String host, String sessionId, int callbackServerPort)
   {
      this.serverBindAddress = host;
      this.sessionId = sessionId;
      this.serverBindPort = callbackServerPort;
   }

   public void handleCallback(Callback callback) throws HandleCallbackException
   {

      System.out.println("Need to make call on SocketCallbackServer with results. " + callback);

      try
      {
         System.out.println("Making callback call to " + serverBindPort);
         Socket socket = new Socket(serverBindAddress, serverBindPort);
         BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
         BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

         ObjectOutputStream oos = new ObjectOutputStream(out);
         ObjectInputStream objInputStream = new ObjectInputStream(in);

         oos.writeObject(callback);
         oos.reset();
         oos.writeObject(Boolean.TRUE);
         oos.flush();
         oos.reset();

         Object obj = objInputStream.readObject();
         objInputStream.readObject();
      }
      catch(IOException e)
      {
         e.printStackTrace();
         throw new HandleCallbackException(e.getMessage(), e);
      }
      catch(ClassNotFoundException e)
      {
         e.printStackTrace();
      }

   }

}