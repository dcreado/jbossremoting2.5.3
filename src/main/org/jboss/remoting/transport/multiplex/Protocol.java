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
 * Created on Jul 23, 2005
 */

package org.jboss.remoting.transport.multiplex;


import org.jboss.logging.Logger;
import org.jboss.remoting.transport.multiplex.utility.StoppableThread;
import org.jboss.remoting.transport.multiplex.utility.VirtualSelector;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>Protocol</code> is responsible for handling internal Multiplex messages.
 * Some of these, for example, the messages involved in creating a
 * new connection (<code>acceptConnect()</code>, <code>connect()</code>,
 * <code>answerConnect()</code>) are synchronous.
 * Others, such as a request to shut down, are received asynchronously by
 * <code>Protocol.BackChannelThread</code>.
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class Protocol
{
   protected static final Logger log = Logger.getLogger(Protocol.class);

   // message types
   public static final int MP_CONNECT 	 	  		   = 0;
   public static final int MP_CONNECTED 	  		   = 1;
   public static final int MP_VERIFY_CONNECTION        = 2;
// public static final int MP_INPUT_SHUTDOWN 		   = 3;
   public static final int MP_OUTPUT_SHUTDOWN 		   = 4;
   public static final int MP_DISCONNECT	  		   = 5;
   public static final int MP_REGISTER_REMOTE_SERVER   = 6;
   public static final int MP_UNREGISTER_REMOTE_SERVER = 7;
   public static final int MP_REQUEST_MANAGER_SHUTDOWN = 8;
   public static final int MP_ERROR 	 	  		   = 9;
   public static final int MP_TRUE					   = 10;
   public static final int MP_FALSE					   = 11;

   /** InputStream used to receive synchronous messages */
   private MultiplexingInputStream protocolInputStream;

   /** OutputStream used by connect() to communicate with a ServerSocket */
   private MultiplexingOutputStream serverSocketOutputStream;

   /** OutputStream for sending messages to remote backChannelInputStream */
   private MultiplexingOutputStream protocolOutputStream;

   private boolean trace;
   private boolean debug;
   private boolean info;


   /**
 * @param virtualSelector
 * @return
 */
   public static BackChannelThread getBackChannelThread(VirtualSelector virtualSelector)
   {
      return new BackChannelThread(virtualSelector);
   }


   /**
    *
    * @param manager
    * @throws IOException
    */
   public Protocol(MultiplexingManager manager) throws IOException
   {
      protocolInputStream = manager.getAnInputStream(SocketId.PROTOCOL_SOCKET_ID, null);
      protocolOutputStream = new MultiplexingOutputStream(manager, SocketId.BACKCHANNEL_SOCKET_ID);
      serverSocketOutputStream = new MultiplexingOutputStream(manager, SocketId.SERVER_SOCKET_ID);
      trace = log.isTraceEnabled();
      debug = log.isDebugEnabled();
      info = log.isInfoEnabled();
   }


   /**
    * @param is
    * @param socketId
    * @return
    * @throws IOException
    */
   public SocketId connect(MultiplexingInputStream is, SocketId socketId) throws IOException
   {
      return connect(is, socketId, 0);
   }


   /**
    * @param is
    * @param socketId
    *
    * @return
    * @throws IOException
    */
   public SocketId connect(MultiplexingInputStream is, SocketId socketId, int timeout) throws IOException
   {
      log.debug("entering Protocol.connect()");
      long start = System.currentTimeMillis();
      int timeLeft = 0;
      int savedTimeout = is.getTimeout();

      synchronized (serverSocketOutputStream)
      {
         serverSocketOutputStream.write(MP_CONNECT);
         if (debug) log.debug("Protocol.connect(): wrote: CONNECT (" + MP_CONNECT + ")");
         serverSocketOutputStream.writeInt(socketId.getPort());
         if (debug) log.debug("Protocol.connect(): wrote port: " + socketId.getPort());
      }

      try
      {
         if (timeout > 0)
            if ((timeLeft = timeout - (int)(System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException();

         is.setTimeout(timeLeft);
         int messageType = is.read();
         if (debug) log.debug("Protocol.connect(): read message type: " + messageType);

         switch (messageType)
         {
            case MP_CONNECTED:
               if (timeout > 0)
                  if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
                     throw new SocketTimeoutException("connect timed out");

               is.setTimeout(timeLeft);
               int remotePort = is.readInt();
               if (debug) log.debug("Protocol.connect(): read port: " + remotePort);
               return new SocketId(remotePort);

            default:
               log.error("Protocol.connect(): expecting a CONNECTED message: received: " + messageType);
            throw new IOException("Protocol.connect(): expecting a CONNECTED message: received: " + messageType);
         }
      }
      catch (SocketTimeoutException e)
      {
         log.info("timeout in Protocol.connect()");
         throw e;
      }
      catch (Exception e)
      {
         log.error(e);
         StackTraceElement[] stes = e.getStackTrace();
         for (int i = 0; i < stes.length; i++)
            log.error(stes[i].toString());
         throw new IOException(e.getMessage());
      }
      finally
      {
         is.setTimeout(savedTimeout);
      }
   }


   /**
    * @param is
    * @param timeout
    *
    * @return
    * @throws IOException
    */
   public SocketId acceptConnect(MultiplexingInputStream is, int timeout) throws IOException
   {
      log.debug("entered acceptConnect()");
      long start = System.currentTimeMillis();
      int timeLeft = timeout;
      int savedTimeout = is.getTimeout();

      try
      {
         is.setTimeout(timeLeft);
         int messageType = is.read();
         if (debug) log.debug("Protocol.acceptConnect(): read message type: " + messageType);

         switch (messageType)
         {
            case MP_CONNECT:
               if (timeout > 0)
                  if ((timeLeft = timeout - (int)(System.currentTimeMillis() - start)) <= 0)
                     throw new SocketTimeoutException();

               is.setTimeout(timeLeft);
               int remotePort = is.readInt();
               if (debug) log.debug("Protocol.acceptConnect(): read port: " + remotePort);
               return new SocketId(remotePort);

            case -1:
               log.info("Protocol.acceptConnect(): end of file");
               throw new EOFException();

            default:
               log.error("Protocol.acceptConnect: expecting a CONNECT message: received: " + messageType);
            throw new IOException("Protocol.acceptConnect: expecting a CONNECT message: received: " + messageType);
         }
      }
      catch (SocketTimeoutException e)
      {
         log.info("timeout in Protocol.acceptConnect()");;
         throw e;
      }
      finally
      {
         is.setTimeout(savedTimeout);
      }
   }


   /**
    * @param os
    * @param port
    *
    * @throws IOException
    */
   public void answerConnect(MultiplexingOutputStream os, int port) throws IOException
   {
      os.write(MP_CONNECTED);
      if (debug) log.debug("Protocol.answerConnect(): wrote: CONNECTED (" + MP_CONNECTED + ")");
      os.writeInt(port);
      if (debug) log.debug("Protocol.answerConnect(): wrote port: " + port);
   }


   /**
    *
    * @param socketId
    */
   public void notifyOutputShutdown(SocketId socketId)
   {
      int port = socketId.getPort();

      try
      {
         synchronized (protocolOutputStream)
         {
            protocolOutputStream.write(MP_OUTPUT_SHUTDOWN, port);
            protocolOutputStream.writeInt(port, port);
         }

         if (debug) log.debug("Protocol.notifyOutputShutdown(): wrote: OUTPUT_SHUTDOWN (" + MP_OUTPUT_SHUTDOWN + ") for port: " + port);
      }
      catch (IOException ignored)
      {
         log.error("Protocol.notifyOutputShutdown(): unable to send MP_OUTPUT_SHUTDOWN message to port: " + port);
      }
   }


   /**
    *
    * @param socketId
    */
   public void disconnect(SocketId socketId)
   {
      int port = socketId.getPort();

      try
      {
         synchronized (protocolOutputStream)
         {
            protocolOutputStream.write(MP_DISCONNECT, port);
            protocolOutputStream.writeInt(port, port);
         }

         if (debug) log.debug("Protocol.disconnect(): wrote: DISCONNECT (" + MP_CONNECTED + ") for port: " + port);
      }
      catch (IOException ignored)
      {
         log.error("Protocol.disconnect(): unable to send DISCONNECT message to port: " + port);
      }
   }


   /**
    * @param timeout
    */
   public void registerRemoteServerSocket(int timeout) throws IOException
   {
      int answer = MP_FALSE;

      synchronized (protocolInputStream)
      {
         synchronized (protocolOutputStream)
         {
            protocolOutputStream.write(MP_REGISTER_REMOTE_SERVER);
         }

         if (debug) log.debug("Protocol.registerRemoteServerSocket(): wrote: REGISTER_REMOTE_SERVER (" + MP_REGISTER_REMOTE_SERVER + ")");
         protocolInputStream.setTimeout(timeout);
         answer = protocolInputStream.read();
      }

      if (debug) log.debug("Protocol.registerRemoteServerSocket(): read: " + (answer == MP_TRUE ? "true" : "false"));

      if (answer == MP_FALSE)
         throw new IOException("unable to register remote socket");
   }


   /**
    *
    */
   public void unregisterRemoteServerSocket()
   {
      log.debug("unregisterRemoteServerSocket()");

      try
      {
         synchronized (protocolOutputStream)
         {
            protocolOutputStream.write(MP_UNREGISTER_REMOTE_SERVER);
         }

         if (debug) log.debug("Protocol.disconnect(): wrote: UNREGISTER_REMOTE_SERVER (" + MP_UNREGISTER_REMOTE_SERVER + ")");
      }
      catch (IOException ignored)
      {
         log.error("Protocol.unregisterRemoteServerSocket(): unable to send UNREGISTER_REMOTE_SERVER");
      }
   }


   public boolean requestManagerShutdown(int timeout) throws IOException
   {
      int b;

      synchronized (protocolInputStream)
      {
         synchronized (protocolOutputStream)
         {
            protocolOutputStream.write(MP_REQUEST_MANAGER_SHUTDOWN, OutputMultiplexor.BRACKETS_ALL);
         }

         if (debug) log.debug("Protocol.requestManagerShutdown(): wrote: REQUEST_MANAGER_SHUTDOWN (" + MP_REQUEST_MANAGER_SHUTDOWN + ")");
         protocolInputStream.setTimeout(timeout);
         b = protocolInputStream.read();
      }

      boolean answer = (b == MP_TRUE) ? true : false;
      if (debug) log.debug("Protocol.requestManagerShutdown(): read: " + answer);
      return answer;
   }


   /**
    *
    */
   static class BackChannelThread extends StoppableThread
   {
      VirtualSelector virtualSelector;
      VirtualSocket socket;


      public BackChannelThread(VirtualSelector virtualSelector)
      {
         this.virtualSelector = virtualSelector;
      }


      /**
       *
       */
      public void shutdown()
      {
         log.debug("back channel thread: beginning shut down");
         super.shutdown();
         virtualSelector.close();
         interrupt();
      }


      /**
       *
       */
      protected void doInit()
      {
         log.debug("back channel thread starting");
      }


      /**
       *
       */
      protected void doRun()
      {
         MultiplexingManager manager = null;
         Map streamMap;
         int messageType;
         int port;
         int answer;

//         while (null == (streamMap = virtualSelector.select()) && running)
//            log.debug("select() loop");
//
//         if (!running)
//            return;

         streamMap = virtualSelector.select();
         if (streamMap == null)
            return;

         Iterator it = streamMap.keySet().iterator();
         while (it.hasNext())
         {
            try
            {
               MultiplexingInputStream is = (MultiplexingInputStream) it.next();

               if (is.available() == 0)
               {
                  log.debug("available == 0");
                  virtualSelector.remove(is);
                  continue;
               }

               manager = (MultiplexingManager) streamMap.get(is);
               if (manager == null)
                  continue;

               OutputStream os = manager.getBackchannelOutputStream();
               messageType = is.read();
               log.debug("back channel thread: read message type: " + messageType);

               switch (messageType)
               {
                  case MP_OUTPUT_SHUTDOWN:

                     port = is.readInt();
                     if (log.isDebugEnabled())
                        log.debug("back channel thread: read OUTPUT_SHUTDOWN for port: " + port);
                     socket = manager.getSocketByLocalPort(new SocketId(port));

                     if (socket == null)
                     {
                        log.info("back channel thread (OUTPUT_SHUTDOWN): unable to retrieve socket at port: " + port);
                     }
                     else
                     {
                        socket.handleRemoteOutputShutDown();
                     }

                     break;

                  case MP_DISCONNECT:

                     port = is.readInt();
                     log.debug("back channel thread: read DISCONNECT for port: " + port);
                     socket = manager.getSocketByLocalPort(new SocketId(port));

                     if (socket == null)
                     {
                        log.info("back channel thread (DISCONNECT): unable to retrieve socket at port: " + port);
                     }
                     else
                     {
                        socket.handleRemoteDisconnect();
                     }

                     break;

                  case MP_REGISTER_REMOTE_SERVER:

                     // remote VirtualServerSocket is starting up
                     log.debug("back channel thread: read REGISTER_REMOTE_SERVER");
                     answer = MP_TRUE;

                     try
                     {
                        manager.registerRemoteServerSocket();
                     }
                     catch (Exception e)
                     {
                        answer = MP_FALSE;
                        log.info("back channel thread: unable to register remote server", e);
                     }

                     os.write(answer);
                     break;

                  case MP_UNREGISTER_REMOTE_SERVER:
                     // remote VirtualServerSocket is shutting down
                     log.debug("back channel thread: read UNREGISTER_REMOTE_SERVER");
                     manager.unRegisterRemoteServerSocket();

                     break;

                  case MP_REQUEST_MANAGER_SHUTDOWN:
                     // remote MultiplexingManager is shutting down
                     log.debug("back channel thread: read REQUEST_MANAGER_SHUTDOWN");
                     answer = manager.respondToShutdownRequest() ? MP_TRUE : MP_FALSE;
                     if (log.isDebugEnabled()) log.debug("back channel thread: writing " + answer);
                     os.write(answer);
                     break;

                  default:
                     log.error("unexpected message type in back channel thread: " + messageType);
               }
            }
            catch (InterruptedIOException e)
            {
               if (isRunning())
                  log.error("back channel thread: i/o interruption", e);
               else
                  log.error("back channel thread: i/o interruption");
            }
            catch (IOException e)
            {
               if (isRunning())
               {
                  log.error("back channel thread: i/o error: " + manager.getSocket().toString(), e);
               }
               else
                  log.error("back channel thread: i/o error");
            }
         }
      }


      /**
       *
       */
      protected void doShutDown()
      {
         log.debug("back channel thread shutting down");
      }

   }
}
