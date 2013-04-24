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

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;

/**
 * A <code>SocketId</code> wraps an integer that identifies an endpoint of a
 * virtual connection.  Each virtual
 * socket group is created with a standard set of system endpoints used
 * for protocol messages.  These have standard negative integer values
 * that are reused in each virtual socket group.  Virtual sockets are
 * created with a <code>SocketId</code> whose value is unique in its JVM.
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 *
 * TODO: verify bytes has no 0's
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class SocketId implements Serializable
{
   protected static final Logger log = Logger.getLogger(SocketId.class);
   private static final String ISO_8859_1 = "ISO-8859-1";

   public static final int PROTOCOL_PORT 			  = -1;
   public static final int SERVER_SOCKET_PORT 		  = -2;
   public static final int SERVER_SOCKET_CONNECT_PORT = -3;
   public static final int SERVER_SOCKET_VERIFY_PORT  = -4;
   public static final int BACKCHANNEL_PORT 		  = -5;
   public static final int DEADLETTER_PORT 			  = -6;
   public static final int INITIAL_OUTPUT_PORT 		  = -7;
   private static final int MIN_PORT				  =  INITIAL_OUTPUT_PORT;
   private static int maxPort = Integer.MAX_VALUE - 1;

   public static final SocketId PROTOCOL_SOCKET_ID 	 	 = new SocketId((short) PROTOCOL_PORT);
   public static final SocketId SERVER_SOCKET_ID 		 = new SocketId((short) SERVER_SOCKET_PORT);
   public static final SocketId SERVER_SOCKET_CONNECT_ID = new SocketId((short) SERVER_SOCKET_CONNECT_PORT);
   public static final SocketId SERVER_SOCKET_VERIFY_ID  = new SocketId((short) SERVER_SOCKET_VERIFY_PORT);
   public static final SocketId BACKCHANNEL_SOCKET_ID 	 = new SocketId((short) BACKCHANNEL_PORT);
   public static final SocketId DEADLETTER_SOCKET_ID 	 = new SocketId((short) DEADLETTER_PORT);
   public static final SocketId INITIAL_OUTPUT_SOCKET_ID = new SocketId((short) INITIAL_OUTPUT_PORT);

   private static HashSet portsInUse = new HashSet();
   private static int nextNewPort = 1;
   private static boolean hasWrapped = false;

   private int port;
   byte[] bytes;
   private static final long serialVersionUID = 1126328489938867931L;

   /**
    *
    */
      public SocketId()
      {
         port = getFreePort();
         setBytes(port);
      }


/**
 *
 * @param port
 */
   public SocketId(int port) throws IOException
   {
      checkPortValue(port);
      this.port = port;
      setBytes(port);
   }


/**
 *
 * @param id
 */
   public SocketId(byte[] bytes) throws IOException
   {
      this.bytes = bytes;
      setPort(bytes);
      checkPortValue(port);
   }


   /**
    *
    * @param port
    */
   protected SocketId(short port)
   {
      this.port = port;
      setBytes(port);
   }


   public byte[] toByteArray()
   {
      return bytes;
   }


   public int getPort()
   {
      return port;
   }


   public void releasePort()
   {
      freePort(port);
   }


   public static void setMaxPort(int max)
   {
      maxPort = max;
   }


   public boolean equals(Object o)
   {
      if (! (o instanceof SocketId))
         return false;

      if (this.port == ((SocketId) o).port)
         return true;
      else
         return false;
   }


   public int hashCode()
   {
      return port;
   }


   public String toString()
   {
      return Integer.toString(port);
   }


   protected void checkPortValue(int port) throws IOException
   {
      if (port < MIN_PORT)
      {
         log.error("attempt to create port with illegal value: " + port);
         throw new IOException("attempt to create port with illegal value: " + port);
      }
   }


   protected void setPort(byte[] bytes)
   {
      try
      {
         port = Integer.parseInt(new String(bytes, ISO_8859_1));
      }

      catch (UnsupportedEncodingException e)
      {
         // ISO-8859-1 should be standard in all Java implementations
         log.fatal("org.jboss.remoting.transport.multiplex.OutputMultiplexor(): charset ISO-8859-1 not found", e);
         throw new RuntimeException("org.jboss.remoting.transport.multiplex.OutputMultiplexor(): charset ISO-8859-1 not found", e);
      }
      catch (RuntimeException r)
      {
         log.error("problem with port: ");
         for (int i = 0; i < bytes.length; i++) log.error("" + bytes[i]);
         throw r;
      }
   }


   protected void setBytes(int port)
   {
      try
      {
         bytes = Integer.toString(port).getBytes(ISO_8859_1);
      }
      catch (UnsupportedEncodingException e)
      {
         // ISO-8859-1 should be standard in all Java implementations
         log.fatal("org.jboss.remoting.transport.multiplex.OutputMultiplexor(): charset ISO-8859-1 not found", e);
         throw new RuntimeException("org.jboss.remoting.transport.multiplex.OutputMultiplexor(): charset ISO-8859-1 not found", e);
      }
   }


   protected static synchronized void freePort(int port)
   {
      portsInUse.remove(new Integer(port));
      SocketId.class.notifyAll();
   }


   protected static synchronized int getFreePort()
   {
      // If we've used all ints and wrapped around, start over at 1.
      if (nextNewPort > maxPort)
      {
         nextNewPort = 1;
         hasWrapped = true;
      }

      if (hasWrapped)
      {
    	 if (portsInUse.size() >= maxPort)
			try
    	    {
			   SocketId.class.wait();
			}
    	    catch (InterruptedException ignored)
    	    {
			}

         while (true)
         {
            int port = nextNewPort > maxPort ? 1 : nextNewPort++;
            Integer portInteger = new Integer(port);

            if (!portsInUse.contains(portInteger))
            {
               portsInUse.add(portInteger);
               return port;
            }
         }
      }
      else
      {
         int port = nextNewPort++;
         Integer portInteger = new Integer(port);
         portsInUse.add(portInteger);
         return port;
      }
   }
}
