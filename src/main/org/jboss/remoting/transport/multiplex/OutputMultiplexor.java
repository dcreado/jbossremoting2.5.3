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


import org.jboss.logging.Logger;
import org.jboss.remoting.transport.multiplex.utility.StoppableThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLException;

/**
 * <code>OutputMultiplexor</code> is one of the key Multiplex classes, responsible for
 * multiplexing multiple byte streams that share a single TCP connection.  It has an
 * inner class that performs this function.
 * <p>
 * The data stream created here consists of a sequence of packets, each consisting of
 * a header, with the format:
 * <p>
 * <table>
 * <tr>
 *   <td><code>byte</code>:  <td>version (current version is 0)
 * </tr>
 * <tr>
 *   <td><code>int</code>:   <td>destination virtual socket id
 * </tr>
 * <tr>
 *   <td><code>short</code>: <td>number of data bytes to follow
 * </tr>
 * </table>
 * <p>
 * followed by the number of data bytes specified in the header.
 * <p>
 * <code>OutputMultiplexor</code> has two fairness constraints that prevent one virtual stream from
 * starving the others.
 * <p>
 * <ol>
 *  <li><code>maxTimeSlice</code> determines the maximum time devoted to writing bytes for a
 *   given virtual connection before going on to process another virtual connection, and
 *  <li><code>maxDataSlice</code> determines the maximum number of bytes written for a given
 *   virtual connection before going on to process another virtual connection.
 *  </ol>
 *
 * <p>
 * For additional information about configuring <code>OutputMultiplexor</code>, please see the
 * documentation at labs.jbos.org.
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class OutputMultiplexor
{
   protected static final Logger log = Logger.getLogger(OutputMultiplexor.class);

   protected static final int BRACKETS_ALL = -1;
   protected static final int BRACKETS_NONE = -2;
   protected static final int HEADER_SIZE = 7;

   private int messagePoolSize;
   private int messageSize;
   private int maxChunkSize;
   private int maxTimeSlice;
   private int maxDataSlice;
   private int maxErrors;

   private Map configuration = new HashMap();
   private Map writeQueues = Collections.synchronizedMap(new HashMap());
   private Map readyQueues = Collections.synchronizedMap(new HashMap());
   private Map previousDestinationIds = Collections.synchronizedMap(new HashMap());
   private Set unregisteredClients = Collections.synchronizedSet(new HashSet());
   private List messagePool;
   private ByteBuffer buffer;
   private byte[] header = new byte[HEADER_SIZE];
   private int errorCount;

   private boolean trace;
   private boolean debug;
   private boolean info;


/**
 * @param configuration
 * @throws IOException
 */
   protected OutputMultiplexor(Map configuration) throws IOException
   {
      this.configuration.putAll(configuration);

      messagePoolSize
         = Multiplex.getOneParameter(configuration,
                                     "messagePoolSize",
                                     Multiplex.OUTPUT_MESSAGE_POOL_SIZE,
                                     Multiplex.OUTPUT_MESSAGE_POOL_SIZE_DEFAULT);
      messageSize
         = Multiplex.getOneParameter(configuration,
                                     "messageSize",
                                     Multiplex.OUTPUT_MESSAGE_SIZE,
                                     Multiplex.OUTPUT_MESSAGE_SIZE_DEFAULT);
      maxChunkSize
         = Multiplex.getOneParameter(configuration,
                                     "maxChunkSize",
                                     Multiplex.OUTPUT_MAX_CHUNK_SIZE,
                                     Multiplex.OUTPUT_MAX_CHUNK_SIZE_DEFAULT);
      maxTimeSlice
         = Multiplex.getOneParameter(configuration,
                                     "maxTimeSlice",
                                     Multiplex.OUTPUT_MAX_TIME_SLICE,
                                     Multiplex.OUTPUT_MAX_TIME_SLICE_DEFAULT);
      maxDataSlice
         = Multiplex.getOneParameter(configuration,
                                     "maxDataSlice",
                                     Multiplex.OUTPUT_MAX_DATA_SLICE,
                                     Multiplex.OUTPUT_MAX_DATA_SLICE_DEFAULT);

      maxErrors
         = Multiplex.getOneParameter(configuration,
                                  "maxErrors",
                                  Multiplex.OUTPUT_MAX_ERRORS,
                                  Multiplex.OUTPUT_MAX_ERRORS_DEFAULT);

      log.debug("messagePoolSize: " + messagePoolSize);
      log.debug("messageSize:     " + messageSize);
      log.debug("maxChunkSize:    " + maxChunkSize);
      log.debug("maxTimeSlice:    " + maxTimeSlice);
      log.debug("maxDataSlice:    " + maxDataSlice);
      log.debug("maxErrors:       " + maxErrors);

      messagePool = Collections.synchronizedList(new ArrayList(messagePoolSize));
      for (int i = 0; i < messagePoolSize; i++)
         messagePool.add(new Message(messageSize));

      buffer = ByteBuffer.allocate(maxChunkSize + HEADER_SIZE);

      trace = log.isTraceEnabled();
      debug = log.isDebugEnabled();
      info = log.isInfoEnabled();
   }


   /**
    * A class implementing this interface can register to be notified when all of its
    * bytes have been processed.
    */
   public interface OutputMultiplexorClient
   {
      void outputFlushed();
   }


/**
 * @return
 */
   public OutputThread getAnOutputThread()
   {
      return new OutputThread();
   }


/**
 * @param manager
 * @param socketId
 * @param content
 * @throws IOException
 */
   public void write(MultiplexingManager manager, SocketId socketId, byte[] content)
   throws IOException
   {
      write(manager, socketId, content, BRACKETS_NONE);
   }


/**
 *
 * @param manager
 * @param socketId
 * @param content
 * @throws InterruptedException
 */
   public void write(MultiplexingManager manager, SocketId socketId, byte[] content, int brackets)
   throws IOException
   {
      log.debug("entering write()");
      if (trace)
      {
         String messageEnd = "";

         if (content.length > 0)
            messageEnd = ": [" + (0xff & content[0]) + "]";

         log.trace("OutputMultiplexor.write(): queueing "
               + content.length + " bytes for \n  manager: " + manager
               + "\n  socket: " + socketId.getPort() + messageEnd);
      }

      if (content.length == 0)
         return;

      synchronized (readyQueues)
      {
         List writeQueue = (List) writeQueues.get(manager);
         if (writeQueue == null)
         {
            log.error("unregistered client: " + manager);
            return;
         }

         synchronized (writeQueue)
         {
            if (!writeQueue.isEmpty())
            {
               Message message = (Message) writeQueue.get(writeQueue.size() - 1);
               if (message.getDestination().equals(socketId) && message.hasCompatibleBrackets(brackets))
               {
                  message.addContent(content);
               }
               else
                  writeQueue.add(getaMessage(socketId, content, brackets));
            }
            else
               writeQueue.add(getaMessage(socketId, content, brackets));
         }

         readyQueues.put(manager, writeQueue);
         readyQueues.notifyAll();
      }
   }


/**
 * Allows a <code>OutputMultiplexorClient</code> to register to be notified when all
 * of its bytes have been processed.
 *
 * @param client
 */
   public void register(OutputMultiplexorClient client)
   {
      if (debug) log.debug("registering: " + client);
      synchronized (writeQueues)
      {
         List writeQueue = Collections.synchronizedList(new LinkedList());
         writeQueues.put(client, writeQueue);
      }
   }


/**
 * Unregisters an <code>OutputMultiplexorClient</code>.
 *
 * @param client
 */
   public void unregister(OutputMultiplexorClient client)
   {
      if (debug) log.debug("unregistering: " + client);
      synchronized (writeQueues)
      {
         List writeQueue = (List) writeQueues.get(client);
         if (writeQueue == null)
         {
            log.debug("attempt to unregister unknown Listener: " + client);
            client.outputFlushed();
            return;
         }

         if (writeQueue.isEmpty())
         {
            writeQueues.remove(client);
            previousDestinationIds.remove(client);
            client.outputFlushed();
         }
         else
         {
            unregisteredClients.add(client);
         }
      }
   }


   protected Message getaMessage(SocketId socketId, byte[] content, int brackets) throws IOException
   {
      Message m = null;

      if (messagePool.isEmpty())
         m = new Message(messageSize);
      else
         m = (Message) messagePool.remove(0);

      m.set(socketId, content, brackets);
      return m;
   }


   protected void releaseMessage(Message m)
   {
      if (messagePool.size() < messagePoolSize)
      {
         messagePool.add(m);
      }
   }

/**
 *
 */
   class OutputThread extends StoppableThread
   {
      private final Logger log = Logger.getLogger(OutputMultiplexor.OutputThread.class);

      private boolean socketIsOpen = true;
      private Map localWriteQueues = new HashMap();
      private Message pendingMessage;


      public OutputThread()
      {
      }


      /**
       *
       */
      public void shutdown()
      {
         super.shutdown();
         interrupt();
      }


      protected void doInit()
      {
         log.debug("output thread starting");
      }


      protected void doRun()
      {
         while (isRunning())
         {
            log.debug("STARTING new output round");
            localWriteQueues.clear();

            // Wait until there is a pending message in some socket group, then get a
            // local copy of the writeQueue Map.
            synchronized (readyQueues)
            {
               while (readyQueues.isEmpty())
               {
                  try
                  {
                     log.debug("waiting");
                     readyQueues.wait();
                  }
                  catch (InterruptedException e)
                  {
                     if (!isRunning())
                        return;
                  }
               }

               localWriteQueues.putAll(readyQueues);
               readyQueues.clear();
            }

            // Process each socket group that has a pending message.
            Iterator it = localWriteQueues.keySet().iterator();
            while (it.hasNext())
            {
               try
               {
                  MultiplexingManager manager = (MultiplexingManager) it.next();
                  List writeQueue = (List) localWriteQueues.get(manager);
                  OutputStream os = manager.getOutputStream();
                  SocketId destination = null;
                  int dataOutCount = 0;
                  long startTime = System.currentTimeMillis();

                  // Process pending messages in one socket group.
                  while (!writeQueue. isEmpty())
                  {
                     long timeSpent = System.currentTimeMillis() - startTime;
                     if (timeSpent > maxTimeSlice || dataOutCount > maxDataSlice)
                     {
                        if (debug)
                        {
                           log.debug("returning queue: data out: " + dataOutCount + ", time spent: " + timeSpent);
                        }
                        synchronized (readyQueues)
                        {
                           readyQueues.put(manager, writeQueue);
                        }
                        break;
                     }

                     pendingMessage = (Message) writeQueue.remove(0);
                     destination =  pendingMessage.getDestination();

                     // The following code, which combines contiguous messages to the same
                     // destination, slightly degraded performance in tests.
//                     while (!writeQueue.isEmpty())
//                     {
//                        if (!destination.equals(((Message) writeQueue.get(0)).getDestination()))
//                              break;
//
//                        Message nextMessage = (Message) writeQueue.remove(0);
//                        int start = nextMessage.getStart();
//                        int length = nextMessage.getLength();
//                        pendingMessage.addContent(nextMessage.getContent(), start, length);
//                     }

                     int start = pendingMessage.getStart();
                     int length = Math.min(pendingMessage.getLength(), maxChunkSize);
                     try
                     {
                        encode(destination, pendingMessage.getContent(), start,
                               length, os, manager.getSocket().getChannel());
                     }
                     catch (ClosedChannelException e)
                     {
                        log.info(e);
                        writeQueue.clear();
                        manager.setWriteException(e);
                        break;
                     }
                     catch (IOException e)
                     {
                        String message = e.getMessage();
                        if ("An existing connection was forcibly closed by the remote host".equals(message) ||
                            "An established connection was aborted by the software in your host machine".equals(message) ||
                            "Broken pipe".equals(message) ||
                            "Connection reset".equals(message) ||
                            "Connection closed by remote host".equals(message) ||
                            "Socket is closed".equals(message)
                            )
                        {
                           log.debug(e);
                           writeQueue.clear();
                           manager.setWriteException(e);
                           break;
                        }
                        else if (++errorCount > maxErrors)
                        {
                           log.error(e);
                           manager.setWriteException(e);
                           throw e;
                        }
                        else
                        {
//                         Haven't reached maxErrors yet
                           throw e;
                        }
                     }

                     // If it's a long message with bytes left over, return to message queue.
                     if (length < pendingMessage.getLength())
                        returnLongMessageToQueue(writeQueue, pendingMessage);
                     else
                        releaseMessage(pendingMessage);

                     dataOutCount += length;
                     pendingMessage = null;

                     if (trace)
                        log.trace("output thread wrote: " + length + " bytes to socket " + destination.getPort());
                  }

                  if (writeQueue.isEmpty() && unregisteredClients.contains(manager))
                  {
                     writeQueues.remove(writeQueue);
                     previousDestinationIds.remove(manager);
                     unregisteredClients.remove(manager);
                     manager.outputFlushed();
                     continue;
                  }

                  previousDestinationIds.put(manager, destination);

                  if (interrupted()) // outside of writeQueue.take()
                     throw new InterruptedException();
               }
               catch (InterruptedException e)
               {
                  handleError("output thread: interrupted", e);
               }
               catch (SocketException e)
               {
                  handleError("output thread: socket exception", e);
               }
               catch (IOException e)
               {
                  handleError("output thread: i/o error", e);
               }
               finally
               {
                  // Indicate that messages for this socket group have been written.
                  it.remove();
               }
            }
         }

         log.debug("output thread: socketIsConnected: " + socketIsOpen);
         log.debug("output thread: running: " + running);
         log.debug("output thread: pendingMessage ==  " + pendingMessage);
      }


      /**
       *
       */
      protected void doShutDown()
      {
         log.debug("output thread shutting down");
      }


      /**
       *
       * @param bytes
       * @param start
       * @param length
       * @param os
       * @param channel
       * @throws IOException
       */
      protected void encode(SocketId destination, byte[] bytes,  int start,
                            int length, OutputStream os, SocketChannel channel)
      throws IOException
      {
         // Create header.
         int port = destination.getPort();

         // Set version.
         header[0] = (byte) 0;

         // Set destination.
         header[1] = (byte) ((port >>> 24) & 0xff);
         header[2] = (byte) ((port >>> 16) & 0xff);
         header[3] = (byte) ((port >>>  8) & 0xff);
         header[4] = (byte) ( port         & 0xff);

         // Set size.
         header[5] = (byte) ((length >> 8) & 0xff);
         header[6] = (byte) ( length       & 0xff);

         if (channel == null)
         {
            os.write(header);
            os.write(bytes, start, length);
            os.flush();
         }
         else
         {
            buffer.clear();
            buffer.put(header);
            buffer.put(bytes, start, length);
            buffer.flip();
            while (buffer.hasRemaining())
               channel.write(buffer);
         }

         if (trace)
         {
            log.trace("encode(): wrote " + length + " bytes to: " + destination);
            log.trace("header: " + header[0] + " " +
                                   header[1] + " " + header[2] + " " + header[3] + " " + header[4] + " " +
                                   header[5] + " " + header[6]);
            for (int i = 0; i < length; i++)
               log.trace("" + (0xff & bytes[i]));
         }
      }


      protected void returnLongMessageToQueue(List writeQueue, Message pendingMessage)
      {
         SocketId destination = pendingMessage.getDestination();
         pendingMessage.markUsed(maxChunkSize);

         synchronized (writeQueue)
         {
            if (!writeQueue.isEmpty())
            {
               ListIterator lit = writeQueue.listIterator();
               boolean processed = false;
               int remotePort = destination.getPort();
               int brackets = pendingMessage.getBrackets();

               while (lit.hasNext())
               {
                  Message message = (Message) lit.next();

                  if (message.brackets(remotePort))
                  {
                     lit.previous();
                     lit.add(pendingMessage);
                     processed = true;
                     break;
                  }

                  if (message.getDestination().equals(destination)
                        && (BRACKETS_NONE == message.getBrackets() || brackets == message.getBrackets()))
                  {
                     pendingMessage.addContent(message.getContent(), message.getStart(), message.getLength());
                     lit.set(pendingMessage);
                     processed = true;
                     break;
                  }
               }

               if (!processed)
               {
                  writeQueue.add(pendingMessage);
               }
            }
            else
            {
               writeQueue.add(pendingMessage);
            }
         }
      }


      /**
       *
       * @param message
       * @param e
       */
      protected void handleError(String message, Throwable e)
      {
         if (log != null)
         {
            if (e instanceof InterruptedException)
            {
               if (trace)
                  log.trace(message, e);
            }
            else
               log.error(message, e);
         }
      }
   }



   /**
    * A <code>Message</code> holds the destination and content of a byte array destined for
    * the endpoint of a virtual connection.
    * <p>
    * It also has a variable <code>brackets</code> which can be used to indicate that this
    * <code>Message</code> should be sent after other <code>Message</code>s to a given
    * destination.  There are three cases:
    * <p>
    * <table>
    *  <tr><td><b>value<td><b>meaning</tr>
    *  <tr>
    *   <td><code>BRACKETS_ALL</code>
    *   <td>all other <code>Message</code>s should preceed this one
    *  </tr>
    *  <tr>
    *   <td><code>BRACKETS_NONE</code>
    *   <td>there are no constraints on this <code>Message</code>
    *  </tr>
    *  <tr>
    *   <td>any other integer <code>x</code>
    *   <td>all other <code>Message</code>s to destination <code>x</code> should
    *       preceed this <code>Message</code>
    *  </tr>
    * </table>
    */
   private static class Message
   {
      private SocketId socketId;
      private ByteArrayOutputStream baos;
      private int start;
      private int length;
      private int brackets;

      public Message(int size)
      {
         baos = new ByteArrayOutputStream(size);
      }

      public void set(SocketId socketId, byte[] content, int brackets) throws IOException
      {
         this.socketId = socketId;
         baos.reset();
         baos.write(content);
         start = 0;
         length = content.length;
         this.brackets = brackets;
      }

      public SocketId getDestination()
      {
         return socketId;
      }

      public byte[] getContent()
      {
         return baos.toByteArray();
      }

      public void addContent(byte[] bytes) throws IOException
      {
         baos.write(bytes);
         length += bytes.length;
      }

      public void addContent(byte[] bytes, int start, int length)
      {
         baos.write(bytes, start, length);
         this.length += length;
      }

      public int getStart()
      {
         return start;
      }

      public int getLength()
      {
         return length;
      }

      public int getBrackets()
      {
         return brackets;
      }

      public void markUsed(int used)
      {
         length -= used;

         if (length <= 0)
         {
            start = 0;
            length = 0;
            baos.reset();
         }
         else
         {
            start += used;
         }
      }

      public boolean brackets(int b)
      {
         if (brackets == BRACKETS_ALL)
            return true;

         if (brackets == BRACKETS_NONE)
            return false;

         return (brackets == b);
      }

      public boolean hasCompatibleBrackets(int b)
      {
         if (brackets == BRACKETS_ALL || b == BRACKETS_NONE)
            return true;

         return (brackets == b);
      }
   }


   public int getMaxChunkSize()
   {
      return maxChunkSize;
   }


   public void setMaxChunkSize(int maxChunkSize)
   {
      this.maxChunkSize = maxChunkSize;
   }


   public int getMessagePoolSize()
   {
      return messagePoolSize;
   }


   public void setMessagePoolSize(int messagePoolSize)
   {
      this.messagePoolSize = messagePoolSize;
   }


   public int getMessageSize()
   {
      return messageSize;
   }


   public void setMessageSize(int messageSize)
   {
      this.messageSize = messageSize;
   }
}