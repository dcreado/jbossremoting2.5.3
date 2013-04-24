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
 * Created on Jul 22, 2005
 */
 
package org.jboss.remoting.transport.multiplex;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

import org.jboss.logging.Logger;

/**
 * <code>MultiplexingOutputStream</code> is the class returned by
 * <code>VirtualSocket.getOutputStream()</code>.  
 * It supports the methods and behavior implemented by the <code>OutputStream</code> returned by
 * <code>java.net.Socket.getOutputStream()</code>.  For more information about the behavior
 * of the methods, see the javadoc for <code>java.io.OutputStream</code>.
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MultiplexingOutputStream extends OutputStream
{
   protected static final Logger log = Logger.getLogger(MultiplexingOutputStream.class);
   private MultiplexingManager manager;
   private OutputMultiplexor outputMultiplexor;
   private VirtualSocket virtualSocket;
   private SocketId socketId;
   private boolean outputShutdown = false;
   private boolean closed = false;
   private IOException writeException;
   
   private static final int OPEN 				= 0;
   private static final int CONNECTION_RESET 	= 1;
   private static final int CLOSED 				= 2;
   private int connectionState = OPEN;

   private byte[] oneByte = new byte[1];
   private byte[] fourBytes = new byte[4];
   
/**
 * 
 * @param manager
 * @param socketId
 */
   public MultiplexingOutputStream(MultiplexingManager manager, SocketId socketId)
   {
      this(manager, null, socketId);
   }
   
   
/**
 * 
 * @param manager
 * @param virtualSocket
 * @param socketId
 */
   public MultiplexingOutputStream(MultiplexingManager manager, VirtualSocket virtualSocket, SocketId socketId)
   {
      this.manager = manager;
      this.virtualSocket = virtualSocket;
      this.socketId = socketId;
      this.outputMultiplexor = manager.getOutputMultiplexor();
   }
   
//////////////////////////////////////////////////////////////////////////////////////////////////
///                  The following methods are required of all OutputStreams                  '///
//////////////////////////////////////////////////////////////////////////////////////////////////

 /*************************************************************************************************
       ok: public void write(int b) throws IOException;
       ok: public void write(byte b[]) throws IOException;
       ok: public void write(byte b[], int off, int len) throws IOException;
           public void flush() throws IOException;
       ok: public void close() throws IOException;
 *************************************************************************************************/

   
/**
 * See superclass javadoc.
 */
   public void close() throws IOException
   {
      log.debug("MultiplexingOutputStream.close() entered");
      
      if (closed)
         return;
         
      closed = true;
      
      if (virtualSocket != null)
         virtualSocket.close();
   }
   
   
/**
 * See superclass javadoc.
 */
   public void flush() throws IOException
   {
      // Could use flush() to raise priority of messages waiting in OutputMultiplexor's write queue.
   }
   
   
/**
 * See superclass javadoc.
 */
   public void write(int i) throws IOException
   {
      checkStatus();
      oneByte[0] = (byte) i;
      outputMultiplexor.write(manager, socketId, oneByte);
   }
   
/**
 * See superclass javadoc.
 */
   public void write(byte[] array) throws IOException, NullPointerException
   { 
      checkStatus();
      outputMultiplexor.write(manager, socketId, array);
   }
   
   
/**
 * See superclass javadoc.
 */
   public void write(byte[] array, int off, int len)
   throws IOException, NullPointerException, IndexOutOfBoundsException
   { 
      checkStatus();

      if (array == null)
         throw new NullPointerException();
      
      if (off < 0 || len < 0 || off + len > array.length)
         throw new IndexOutOfBoundsException();
      
      byte[] subArray = new byte[len];
      
      for (int i = 0; i < len; i++)
         subArray[i] = array[off + i];

      outputMultiplexor.write(manager, socketId, subArray);
   }
   
  
//////////////////////////////////////////////////////////////////////////////////////////////////
///              The following methods are specific to MultiplexingOutputStream               '///
//////////////////////////////////////////////////////////////////////////////////////////////////

   protected void setWriteException(IOException e)
   {
      writeException = e;
   }
   
   
/**
 * <code>writeInt()</code> is borrowed from <code>DataOutputStream</code>.
 * It saves the extra expense of creating a <code>DataOutputStream</code>. 
 * @param i
 * @throws IOException
 */
   public void writeInt(int i) throws IOException
   {
      fourBytes[0] = (byte) ((i >>> 24) & 0xff);
      fourBytes[1] = (byte) ((i >>> 16) & 0xff);
      fourBytes[2] = (byte) ((i >>>  8) & 0xff);
      fourBytes[3] = (byte) ((i >>>  0) & 0xff);
      outputMultiplexor.write(manager, socketId, fourBytes);
   }
   
/**
 * Determines how to handle a write request depending on what this socket knows about the state
 * of the peer socket.
 * <p>
 * Once this socket knows that the peer socket has closed, no more write requests will be honored.
 * There seem to be two ways for a socket A to learn that its peer socket B has closed.
 * <p>
 * <ol>
 *  <li>If socket A has executed a write, but no subsequent write is performed on B,
 *      then the acknowledgement of the write will carry back the information that B has closed.
 *  <li>If socket B has no pending acknowledgements to send at the time it closes, and then socket A
 *      does a write after B has closed, the (negative) acknowledgement of the write will carry back
 *      the information that B has closed.
 * </ol>
 * <p>
 * Java seems to respond differently to the two cases.  The first write after this socket has learned
 * of the peer's closing through the first scenario will result in a SocketException("Connection reset").
 * In the second scenario, the first write by this socket after the peer has closed 
 * will quietly fail (no exception is thrown).  All subsequent writes after either of these two 
 * scenarios will result in a  SocketException("Broken pipe").
 * <p>
 * Currently, MultiplexingOutputStream implements only a simplified version of this behavior.  In
 * particular, it allows in all cases one write to silently fail, after which all writes result in a
 * SocketException("Broken pipe");
 * <p>
 * Note.  This discussion is based on empirical observation on a linux system, not on examination of code.
 * Your mileage may vary.
 */
   protected void checkStatus() throws IOException
   {
      if (closed)
         throw new SocketException("Socket closed");
      
      if (outputShutdown)
         throw new SocketException("Broken pipe");
      
      if (writeException != null)
         throw writeException;
      
      switch (connectionState)
      {
         case OPEN:
            return;

         case CONNECTION_RESET:
            connectionState = CLOSED;
            return;
            
         case CLOSED:
            throw new SocketException("Broken pipe");
            
         default:
            log.error("unrecognized connection state: " + connectionState);
      }
   }
   
   
/**
 * 
 */
   protected void handleRemoteDisconnect()
   {
      log.debug("entering handleRemoteDisconnect()");
      
      switch (connectionState)
      {
         case OPEN:
            connectionState = CONNECTION_RESET;
            return;
            
         default:
            connectionState = CLOSED;
            log.error("invalid connection state in handleRemoteDisconnect(): " + connectionState);
      }
   }
   
   
/**
 * 
 */
   protected void shutdown()
   {
      outputShutdown = true;
   }
   
   
/**
 * @param i
 * @param brackets
 * @throws IOException
 */
   protected void write(int i, int brackets) throws IOException
   {
      log.debug("brackets: " + brackets);
      oneByte[0] = (byte) i;
      outputMultiplexor.write(manager, socketId, oneByte, brackets);
   }
   
   
/**
 * @param i
 * @param brackets
 * @throws IOException
 */
   protected void writeInt(int i, int brackets) throws IOException
   {
      log.debug("brackets: " + brackets);
      fourBytes[0] = (byte) ((i >>> 24) & 0xff);
      fourBytes[1] = (byte) ((i >>> 16) & 0xff);
      fourBytes[2] = (byte) ((i >>>  8) & 0xff);
      fourBytes[3] = (byte) ((i >>>  0) & 0xff);
      outputMultiplexor.write(manager, socketId, fourBytes, brackets);
   }
}
