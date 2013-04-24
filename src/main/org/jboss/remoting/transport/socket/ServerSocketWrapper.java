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

package org.jboss.remoting.transport.socket;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerSocketWrapper extends ClientSocketWrapper
{
   final static private Logger log = Logger.getLogger(ServerSocketWrapper.class);

   public ServerSocketWrapper(Socket socket) throws Exception
   {
      super(socket);
   }

   public ServerSocketWrapper(Socket socket, Map metadata, Integer timeout) throws Exception
   {
      super(socket, metadata, timeout);
   }

   public synchronized void close() throws IOException
   {
      if(socket != null)
      {
         try
         {
            getOutputStream().write(CLOSING);
            getOutputStream().write(CLOSING);
            getOutputStream().flush();
            log.trace(this + " wrote CLOSING");
         }
         catch (IOException e)
         {
            log.trace(this + " unable to writing CLOSING byte", e);
         }
         
         super.close();
         socket = null;
      }
   }
   
   public void checkConnection() throws IOException
   {
      // Perform acknowledgement to convince client
      // that the socket is still active
      int ACK = 0;
      //long startWait = System.currentTimeMillis();
      try
      {
//         ACK = ((ObjectInputStream) getInputStream()).readByte();
         ACK = getInputStream().read();
      }
      catch(EOFException eof)
      {
         log.trace("EOFException waiting on ACK in read().");
         throw eof;
      }
      catch(IOException e)
      {
         log.debug("IOException when reading in ACK: " + e.getMessage());
         log.trace("IOException when reading in ACK", e);
         throw e;
      }

      if(log.isTraceEnabled())
      {
         log.trace("acknowledge read byte: " + ACK + ": " + Thread.currentThread());
      }

//      ObjectOutputStream out = (ObjectOutputStream) getOutputStream();
      OutputStream out = getOutputStream();

//      out.writeByte(ACK);
//      out.flush();
//      out.reset();

      out.write(ACK);
      out.flush();
   }

   public String toString()
   {
      Socket socket = getSocket();
      return "ServerSocketWrapper[" + socket + "." +
         Integer.toHexString(System.identityHashCode(socket)) + "]";
   }

}
