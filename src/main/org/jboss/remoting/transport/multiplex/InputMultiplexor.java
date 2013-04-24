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

package org.jboss.remoting.transport.multiplex;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.multiplex.utility.StoppableThread;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLException;

/**
 * <code>InputMultiplexor</code> is one of the key Multiplex classes, responsible for
 * demultiplexing multiple byte streams sharing a single TCP connection.  It has two
 * inner classes which can perform this function.  <code>MultiGroupInputThread</code> can perform
 * demultiplexing for any number of NIO sockets, taking advantage of the <code>Selector</code>
 * facility.  For non-NIO sockets, notably SSL sockets, <code>SingleGroupInputThread</code>
 * handles demultiplexing for a single socket.
 * <p>
 * The data stream, created at the other end of the TCP connection by the
 * <code>OutputMultiplexor</code> class, consists of a sequence of packets, each consisting of
 * a header, giving version, destination virtual socket, and number of bytes. followed
 * by the specified number of data bytes. (See <code>OutputMultiplexor</code> for the
 * header format.
 * Each of the demultiplexing thread classes reads a header and transfers the
 * following bytes to the input stream of the target virtual socket.
 *
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class InputMultiplexor
{
   protected static final Logger log = Logger.getLogger(InputMultiplexor.class);
   private static final int HEADER_LENGTH = 7;

   private int bufferSize;
   private int maxErrors;


   public InputMultiplexor(Map configuration)
   {
      bufferSize
         = Multiplex.getOneParameter(configuration,
                                    "bufferSize",
                                    Multiplex.INPUT_BUFFER_SIZE,
                                    Multiplex.INPUT_BUFFER_SIZE_DEFAULT);

      maxErrors
         = Multiplex.getOneParameter(configuration,
                                     "maxErrors",
                                     Multiplex.INPUT_MAX_ERRORS,
                                     Multiplex.INPUT_MAX_ERRORS_DEFAULT);
   }


   /**
    * Returns a <code>MultiGroupInputThread</code> designed to handle multiple virtual socket groups.
    * @param configuration
    * @return a <code>MultiGroupInputThread</code> designed to handle multiple virtual socket groups
    */
   public MultiGroupInputThread getaMultiGroupInputThread() throws IOException
   {
      return new MultiGroupInputThread();
   }


   /**
    * Returns a <code>SingleGroupInputThread</code> designed to handle a single virtual socket group.
    * @return a <code>SingleGroupInputThread</code> designed to handle a single virtual socket group
    */
   public SingleGroupInputThread getaSingleGroupInputThread(MultiplexingManager manager, Socket socket, OutputStream os) throws IOException
   {
      return new SingleGroupInputThread(manager, socket, os);
   }


   public class MultiGroupInputThread extends StoppableThread
   {
      private static final String errMsg1 = "An existing connection was forcibly closed by the remote host";
      private static final String errMsg2 = "An established connection was aborted by the software in your host machine";

      private Map managerProcessorMap;
      private Set socketGroupsToBeRegistered = new HashSet();
      private Set tempSocketGroupSet = new HashSet();
      private boolean socketGroupsAreWaiting;
      private Selector selector;
      private ByteBuffer buffer;
      private byte[] data;

      private boolean trace;
      private boolean debug;
      private boolean info;


      public MultiGroupInputThread() throws IOException
      {
         managerProcessorMap = Collections.synchronizedMap(new HashMap());
         selector = Selector.open();
         buffer = ByteBuffer.allocate(bufferSize);
         data = new byte[bufferSize];

         trace = log.isTraceEnabled();
         debug = log.isDebugEnabled();
         info = log.isInfoEnabled();
      }


      /**
       * Registers manager and socket with NIO Selector
       * @param manager <code>MultiplexingManager</code>
       * @return
       * @throws <code>IOException</code>
       */
      public void registerSocketGroup(MultiplexingManager manager) throws IOException
      {
         if (debug) log.debug(" accepting socket group for registration: " + manager);

         synchronized (socketGroupsToBeRegistered)
         {
            socketGroupsToBeRegistered.add(manager);
            socketGroupsAreWaiting = true;
         }
      }


      protected void doRegistration()
      {
         tempSocketGroupSet.clear();
         synchronized(socketGroupsToBeRegistered)
         {
            tempSocketGroupSet.addAll(socketGroupsToBeRegistered);
            socketGroupsToBeRegistered.clear();
            socketGroupsAreWaiting = false;
         }

         Iterator it = tempSocketGroupSet.iterator();
         while (it.hasNext())
         {
            MultiplexingManager manager = (MultiplexingManager) it.next();
            GroupProcessor groupProcessor = new GroupProcessor(manager);
            SelectableChannel channel = manager.getSocket().getChannel();

            try
            {
               SelectionKey key = channel.register(selector, SelectionKey.OP_READ, groupProcessor);
               groupProcessor.setKey(key);
               managerProcessorMap.put(manager, groupProcessor);
            }
            catch (IOException e)
            {
               // channel might be closed.
               log.warn(e);
            }
         }
      }


      /**
       * Removes references to virtual socket group.
       * @param manager
       */
      public void unregisterSocketGroup(MultiplexingManager manager)
      {
         // Leave GroupProcessor in Map until SelectionKey is cancelled.
         GroupProcessor groupProcessor = (GroupProcessor) managerProcessorMap.get(manager);
         if(groupProcessor == null)
         {
            log.debug("attempting to unregister unknown MultiplexingManager: " + manager);
            return;
         }

         SelectionKey key = groupProcessor.getKey();
         key.cancel();
         managerProcessorMap.remove(manager);
         if (debug) log.debug("unregistered socket group:" + manager);
      }


      public void shutdown()
      {
         // in case thread is still reading
         super.shutdown();
         try
         {
            selector.close();
         }
         catch (IOException e)
         {
            log.error("unable to close selector", e);
         }
         interrupt();
      }


      protected void doInit()
      {
         log.debug("MultiGroupInputThread thread starting");
      }


      protected void doRun()
      {
         log.debug("entering doRun()");
         Set keys = null;

         try
         {
            while (true)
            {
               if (!running)
                  return;

               if (socketGroupsAreWaiting)
                  doRegistration();

               selector.select(200);
               keys = selector.selectedKeys();

               if (!keys.isEmpty())
                  break;
            }
         }
         catch (IOException e)
         {
            log.info(e);
         }
         catch (ClosedSelectorException e)
         {
            log.info("Selector is closed: shutting down input thread");
            super.shutdown();
            return;
         }

         if (trace)
         {
            log.trace("keys: " + selector.keys().size());
            log.trace("selected keys: " + keys.size());
         }

         Iterator it = keys.iterator();
         while (it.hasNext())
         {
            SelectionKey key = (SelectionKey) it.next();
            it.remove();
            GroupProcessor groupProcessor = (GroupProcessor) key.attachment();

            if (groupProcessor == null)
            {
               if (key.isValid())
                  log.error("valid SelectionKey has no attachment: " + key);

               continue;
            }

            groupProcessor.processChannel(key);
         }
      }


      protected void doShutDown()
      {
         log.debug("MultiGroupInputThread shutting down");
      }


      class GroupProcessor
      {
         // Message header
         private byte[] b = new byte[HEADER_LENGTH];
         private int    headerCount;
         private byte   version;
         private int    destination;
         private short  size;

         private MultiplexingManager manager;
         private OutputStream   outputStream;
         private SelectionKey key;
         private int errorCount;


         public GroupProcessor(MultiplexingManager manager)
         {
            this.manager = manager;
         }

         public void processChannel(SelectionKey key)
         {
            log.debug("processChannel()");
            SocketChannel channel = (SocketChannel) key.channel();
            buffer.clear();

            try
            {
               if (channel.read(buffer) < 0)
                     throw new EOFException();

               buffer.flip();

               if (debug)
                  log.debug("read: " + buffer.remaining());

               while (buffer.hasRemaining())
               {
                  if (headerCount < HEADER_LENGTH || size == 0)
                  {
                     // then prepare to process next virtual stream.
                     completeHeader(buffer);

                     if (headerCount < HEADER_LENGTH)
                        return;

                     SocketId socketId = new SocketId(destination);
                     outputStream = manager.getOutputStreamByLocalSocket(socketId);
                     if (outputStream == null)
                     {
                        // We'll get an OutputStream to stash these bytes, just in case they
                        // are coming from a valid source and the local VirtualSocket is still
                        // getting set up.
                        log.info("unknown socket id: " + destination);
                        outputStream = manager.getConnectedOutputStream(socketId);
                     }

                     if (!buffer.hasRemaining())
                        return;
                  }

                  int n = Math.min(size, buffer.remaining());
                  buffer.get(data, 0, n);
                  outputStream.write(data, 0, n);

                  if (trace)
                  {
                     log.trace("received " + n + " bytes for socket: " + destination);
                     for (int i = 0; i < n; i++)
                        log.trace("" + (0xff & data[i]));
                  }

                  size -= n;
                  if (size == 0)
                     headerCount = 0;
               }
            }
            catch (IOException e)
            {
               handleChannelException(e, key, channel);
            }
            catch (Throwable t)
            {
               log.error("doRun()");
               log.error(t);
            }
         }

         public SelectionKey getKey()
         {
            return key;
         }

         public void setKey(SelectionKey key)
         {
            this.key = key;
         }

         private void completeHeader(ByteBuffer bb) throws IOException
         {
            int n = Math.min(bb.remaining(), HEADER_LENGTH - headerCount);
            bb.get(b, headerCount, n);
            headerCount += n;

            if (headerCount == HEADER_LENGTH)
            {
               version = b[0];
               destination =                 (b[1] << 24) | (0x00ff0000 & (b[2] << 16)) |
                               (0x0000ff00 & (b[3] << 8)) | (0x000000ff & b[4]);
               size = (short) ((0x0000ff00 & (b[5] << 8)) | (0x000000ff & b[6]));


               if (size  < 0 || bufferSize < size)
                  throw new CorruptedStreamException("invalid chunk size read on: " + manager + ": "+ size);

               if (version != 0)
                  throw new CorruptedStreamException("invalid version read on: " + manager + ": " + version);
            }
         }

         private void handleChannelException(IOException e, SelectionKey key, SocketChannel channel)
         {
            try
            {
               if (!channel.isOpen())
               {
                  key.cancel();
                  return;
               }

               if (e instanceof EOFException)
               {
                  key.cancel();
                  manager.setEOF();
                  log.debug(e);
                  return;
               }
               
               if (e instanceof SSLException)
               {
                  key.cancel();
                  log.error(e);
                  return;
               }

               if (++errorCount > maxErrors)
                 {
                    manager.setReadException(e);
                    channel.close();
                    key.cancel();
                    log.error(e);
                    log.error("error count exceeds max errors: " + errorCount);
                    return;
                 }

               Socket socket = channel.socket();
               String message = e.getMessage();

               if (socket.isClosed() || socket.isInputShutdown() ||
                   errMsg1.equals(message) || errMsg2.equals(message) ||
                   e instanceof CorruptedStreamException)
               {
                  manager.setReadException(e);
                  channel.close();
                  key.cancel();
                  log.info(e);
                  return;
               }

               // Haven't reached maxErrors yet
               log.warn(e);
            }
            catch (IOException e2)
            {
               log.error("problem closing channel: "  + manager, e2);
            }
         }

         public int          getDestination()   {return destination;}
         public short        getSize()          {return size;}
         public byte         getVersion()       {return version;}
         public OutputStream getOutputStream()  {return outputStream;}
      }
   }


   class SingleGroupInputThread extends StoppableThread
   {
      private InputStream is;
      private OutputStream currentOutputStream;
      private byte[] dataBytes = new byte[bufferSize];
      private MultiplexingManager manager;
      private int dataInCount = 0;
      private int errorCount;
      private boolean eof;

      // Message header
      private byte[] headerBytes = new byte[HEADER_LENGTH];
      private int    headerCount;
      private byte   version;
      private int    destination;
      private short  size;

      private boolean trace;
      private boolean debug;
      private boolean info;


      public SingleGroupInputThread(MultiplexingManager manager, Socket socket, OutputStream os)
      throws IOException
      {
         this.is = new BufferedInputStream(socket.getInputStream());
         this.manager = manager;
         currentOutputStream = os;

         trace = log.isTraceEnabled();
         debug = log.isDebugEnabled();
         info = log.isInfoEnabled();
      }


      public void shutdown()
      {
         // in case thread is still reading
         super.shutdown();
         log.info("interrupting input thread");
         interrupt();
      }


      /**
       *
       */
      protected void doInit()
      {
         log.debug("SingleGroupInputThread thread starting");
      }


      /**
       *
       */
      protected void doRun()
      {
         try
         {
            // end of file
            if (!completeHeader())
            {
               eof = true;
               return;
            }

            SocketId socketId = new SocketId(destination);
            currentOutputStream = manager.getOutputStreamByLocalSocket(socketId);
            if (currentOutputStream == null)
            {
               // We'll get an OutputStream to stash these bytes, just in case they
               // are coming from a valid source and the local VirtualSocket is still
               // getting set up.
               log.info("unknown socket id: " + destination);
               currentOutputStream = manager.getConnectedOutputStream(socketId);
            }

            int bytesRead = 0;
            while (bytesRead < size)
            {
               int n = is.read(dataBytes, 0, size - bytesRead);
               if (n < 0)
               {
                  eof = true;
                  return;
               }

               currentOutputStream.write(dataBytes, 0, n);
               bytesRead += n;

               if (trace)
               {
                  for (int i = 0; i < n; i++)
                     log.trace("" + dataBytes[i]);
               }
            }
         }
         catch (SSLException e)
         {
            log.debug(e.getMessage());
         }
         catch (EOFException e)
         {
            eof = true;
            log.info("end of file");
         }
         catch (IOException e)
         {
            if (++errorCount > maxErrors)
            {
               manager.setReadException(e);
               super.shutdown();
               log.error(e);
            }
            else
               log.warn(e);
         }
         finally
         {
            if (eof)
            {
               super.shutdown();
               manager.setEOF();
            }
         }
      }


      private boolean completeHeader() throws IOException
      {
         while (headerCount < HEADER_LENGTH)
         {
            int n = is.read(headerBytes, headerCount, HEADER_LENGTH - headerCount);

            // end of file
            if (n < 0)
               return false;

            headerCount += n;
         }

         // Reset for next header.
         headerCount = 0;

         version = headerBytes[0];
         destination =               (headerBytes[1] << 24) | (0x00ff0000 & (headerBytes[2] << 16)) |
                       (0x0000ff00 & (headerBytes[3] << 8)) | (0x000000ff & headerBytes[4]);
         size = (short) ((0x0000ff00 & (headerBytes[5] << 8)) | (0x000000ff & headerBytes[6]));

         if (trace)
         {
            log.trace("version:     " + version);
            log.trace("destination: " + destination);
            log.trace("size:        " + size);
         }

         if (size  < 0 || bufferSize < size)
            throw new CorruptedStreamException("invalid chunk size read on: " + manager + ": "+ size);

         if (version != 0)
            throw new CorruptedStreamException("invalid version read on: " + manager + ": " + version);

         return true;
      }


      protected void doShutDown()
      {
         log.debug("input thread: data bytes read: " + dataInCount);
         log.debug("input thread shutting down");
      }
   }


   private static class CorruptedStreamException extends IOException
   {
      CorruptedStreamException(String message) {super(message);}
   }
}