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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.multiplex.utility.GrowablePipedInputStream;
import org.jboss.remoting.transport.multiplex.utility.GrowablePipedOutputStream;
import org.jboss.remoting.transport.multiplex.utility.VirtualSelector;

/**
 * <code>MultiplexingInputStream</code> is the class returned by
 * <code>VirtualSocket.getInputStream()</code>.  
 * It supports the methods and behavior implemented by the <code>InputStream</code> returned by
 * <code>java.net.Socket.getInputStream()</code>.  For more information about the behavior
 * of the methods, see the javadoc for <code>java.io.InputStream</code>.
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MultiplexingInputStream extends GrowablePipedInputStream
{
   protected static final Logger log = Logger.getLogger(MultiplexingInputStream.class);
   private VirtualSocket socket;
   private boolean eof = false;
   private boolean closed = false;
   private boolean remoteShutDownPending = false;
   private Set readingThreads = new HashSet();
   private IOException readException;
   private long skipCount = 0;
   private boolean tracing;

   
/**
 * @param sourceStream 
 * @param manager 
 */
   public MultiplexingInputStream(GrowablePipedOutputStream sourceStream, MultiplexingManager manager)
   throws IOException
   {
      this(sourceStream, manager, null, null);
   }
   
   
/**
  * @param sourceStream
  * @param manager 
  * @param socket 
  * 
  */
   public MultiplexingInputStream(GrowablePipedOutputStream sourceStream,
                                  MultiplexingManager manager,
                                  VirtualSocket socket)
   throws IOException
   {
      this(sourceStream, manager, socket, null);
   }
   
      
/**
 * @param sourceStream
 * @param manager 
 * @param socket
 * @param virtualSelector 
 * 
 */
   public MultiplexingInputStream(GrowablePipedOutputStream sourceStream,
                                  MultiplexingManager manager,
                                  VirtualSocket socket,
                                  VirtualSelector virtualSelector)
   throws IOException
   {
      super(sourceStream, virtualSelector);
      this.socket = socket;
      tracing = log.isTraceEnabled();
   }
      
   
//////////////////////////////////////////////////////////////////////////////////////////////////
///                 The following methods are required of all InputStreams                    '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/*************************************************************************************************
      ok: public int read() throws IOException;
      ok: public int read(byte b[]) throws IOException;
      ok: public int read(byte b[], int off, int len) throws IOException;
      ok: public long skip(long n) throws IOException;
      ok: public int available() throws IOException;
      ok: public void close() throws IOException;
      ok: public void mark(int readlimit);
      ok: public void reset() throws IOException;
      ok: public boolean markSupported();
*************************************************************************************************/
      
/**
 * See superclass javadoc.
 */
   public void close() throws IOException
   {
      if (closed)
         return;
      
      log.debug("MultiplexingInputStream closing");
      closed = true;
      super.close();
      
      if (socket != null)
         socket.close();

      // If a thread is currently in read(), interrupt it.
      interruptReadingThreads();
   }

   
/**
 * See superclass javadoc.
 */
   public synchronized int read() throws IOException
   {
      if (eof)
         return -1;
      
      if (closed)
         throw new SocketException("Socket closed");
      
      if (readException != null)
         throw readException;
   
      if (skipCount > 0)
         skip(skipCount);

      try
      {
         // We leave a reference to the current thread so that close() and handleRemoteShutdown()
         // can interrupt it if necessary.
         readingThreads.add(Thread.currentThread());
         int b = super.read();
         readingThreads.remove(Thread.currentThread());
         
         if (tracing)
            log.trace("read(): super.read() returned: " + b);

         if (remoteShutDownPending && available() == 0)
            setEOF();
            
         return b & 0xff;
      }
      catch (IOException e)
      {
         readingThreads.remove(Thread.currentThread());
         
         if (closed)
            throw new SocketException("Socket closed");

         if (eof)
            return -1;
         
         if (readException != null)
            throw readException;

         throw e;
      }
   }
   
   
/**
 * See superclass javadoc.
 */
   public int read(byte[] bytes) throws IOException
   {
      return read(bytes, 0, bytes.length);
   }
   
   
/**
 * See superclass javadoc.
 */
   public synchronized int read(byte[] bytes, int off, int len) throws IOException
   {      
      log.trace("entering read()");
      
      if (eof)
         return -1;
      
      if (closed)
         throw new SocketException("Socket closed");
      
      if (readException != null)
         throw readException;

      if (skipCount > 0)
         skip(skipCount);
      
      try
      {
         // We leave a reference to the current thread so that handleRemoteShutdown() can
         // interrupt it if necessary.
         readingThreads.add(Thread.currentThread());
         int n = super.read(bytes, off, len);
         readingThreads.remove(Thread.currentThread());
         
         if (tracing)
            log.trace("super.read() returned " + n + " bytes: "
                  + "[" + (0xff & bytes[off]) + ".." + (0xff & bytes[off+n-1]) + "]");

         if (remoteShutDownPending && available() == 0)
            setEOF();
         
         return n;
      }
      catch (IOException e)
      {
         readingThreads.remove(Thread.currentThread());
         
         if (eof)
            return -1;
         
         if (closed)
            throw new SocketException("Socket closed");

         throw e;
      }
   }
   
   
   /**
    * See superclass javadoc.
    */
   public synchronized long skip(long n) throws IOException
   {
      if (eof)
         return 0;
      
      if (closed)
         throw new SocketException("Socket closed");
      
      if (readException != null)
         throw readException;

      if (n <= 0)
         return 0;
      
      int skipped = 0;
      
      try
      {
         readingThreads.add(Thread.currentThread());
         
         while (skipped < n && (skipped == 0 || available() > 0))
         {
            if (read() == -1)
               break;
            
            skipped++;
         }
         
         readingThreads.remove(Thread.currentThread());
         
         if (remoteShutDownPending && available() == 0)
            setEOF();
         
         return skipped;
      }
      catch (IOException e)
      {
         readingThreads.remove(Thread.currentThread());
         
         if (eof)
            return -1;
         
         if (closed)
            throw new SocketException("Socket closed");

         throw e;
      }
   }

   
//////////////////////////////////////////////////////////////////////////////////////////////////
///              The following methods are specific to MultiplexingInputStream                '///
//////////////////////////////////////////////////////////////////////////////////////////////////
 
/**
 * 
 */
   protected VirtualSocket getSocket()
   {
      return socket;
   }
   
   
/**
 * <code>handleRemoteShutdown()</code> is responsible for informing the <code>MultiplexingInputStream</code>
 * that no more bytes will be coming from the remote <code>MultiplexingOutputStream</code> to which
 * it is connected, because <code>shutdownOutput()</code> or <code>close()</code> has been called on the 
 * remote <code>VirtualSocket</code>.  The result is that once all bytes sent by the remote socket have
 * been consumed, all subsequent calls to read() will return -1 and all subsequent calls to skip() will
 * return 0, indicating end of file has been reached.
 */
   protected synchronized void handleRemoteShutdown() throws IOException
   {
   /*
    * handleRemoteShutdown() needs to handle two cases correctly:
    *
    * Case 1. all bytes transmitted by the remote MultiplexingOutputStream have been consumed by the time
    *   handleRemoteShutdown() executes, and
    * 
    * Case 2. not all bytes transmitted by the remote MultiplexingOutputStream have been consumed
    *   by the time handleRemoteShutdown() executes..
    *
    * Correctness argument:
    * 
    * Case 1. 
    * 
    * The bracketing facility implemented by OutputMultiplexor guarantees that all bytes
    * transmitted by the remote MultiplexingOutputStream will arrive and be stored in this
    * MultiplexingInputStream before the protocol message arrives that leads to 
    * handleRemoteShutdown() being called.  Therefore, if available() == 0 is true upon entering
    * handleRemoteShutdown(), all transmitted bytes have been consumed and it is correct to indicate
    * that this MultiplexingInputStream is at end of file.  
    * 
    * Case 1a. No threads are currently in read() or skip():
    * 
    *   Calling setEOF() will guarantee that all subsequent calls to read() will return -1 and all
    *   subsequent calls to skip() will return 0.
    * 
    * Case 1b. One or more threads are currently in read() or skip():
    * 
    *   Since all read() methods, skip(), and handleRemoteShutdown() are synchronized, the only way
    *   handleRemoteShutdown() can be executing is if all of the threads in read() and skip() are
    *   blocked on the wait() call in super.read().  Then all of the blocked threads are referenced in
    *   the Set readingThreads, and calling interruptReadingThreads will guarantee that they are 
    *   interrupted, which will lead to their throwing an InterruptedIOException.  Moreover, calling
    *   setEOF() will guarantee that all such threads will see eof == true in the exception  and
    *   will return -1. If any of the threads made the call to read() by way of skip(), then the
    *   condition (skipped == 0 || available() > 0) was true when read() was called, and since we
    *   are assumeing available() == 0, then skipped == 0 must have been true.  Therefore, when 
    *   it gets -1 from the call to read(), skip() will return 0.  Finally, calling setEOF() in
    *   handleRemoteShutdown() will guarantee that all subsequent calls to read() will return -1
    *   and all subsequent calls to skip() will return 0.
    * 
    * Case 2.
    * 
    * Suppose, on the other hand, that available() == 0 is false.  Then the only action taken by
    * handleRemoteShutdown() is to set remoteShutdownPending to true, and as long as bytes are
    * available, there is no obstacle to their being read or skipped.
    * 
    * Fact.  The last transmitted byte has been consumed if and only if available() == 0.
    * 
    * (If): 
    * The fact that handleRemoteShutdown() has been called implies that all bytes transmitted from the remote
    * socket have already arrived and been stored in this MultiplexingInputStream, so the value returned
    * by available() will decrease monotonically, and  once available() == 0, the last transmitted byte has
    * been consumed.  
    * 
    * (Only if):
    * This direction is obvious.
    * 
    * Now, if no thread ever requests the last available byte to be read or skipped, then all calls to read()
    * or skip() following the call to handleRemoteShutdown() will execute with available() > 0, and there
    * will be no impediment to their successful completion.
    * 
    * Suppose, then, that some thread T makes a call to read() that retrieves the last available byte.
    * Upon returning from super.read(), T will find (remoteShutdownPending && available() == 0) is true,
    * and it will call setEOF(), which, by the Fact argued above, is a correct action. Since available()
    * was > 0 when the T entered read(), T will never call wait() in super.read(), so no other thread will
    * enter read() or skip() until T leaves read().  At that point any threads entering read() will find
    * eof == true and will return -1, and any threads entering skip() will find eof == true and return 0.
    *
    * Finally, suppose that some thread T makes a call to skip() to skip the last available byte.
    * T will eventually leave the while loop in skip() with available() == 0, and when it reaches
    * the test for (remoteShutDownPending && available() == 0), it will call setEOF().
    * Since available() was > 0 when the T entered skip(), T will never call wait() in super.read(),
    * so no other thread will enter read() or skip() until T leaves skip().  At that point any threads
    * entering read() will find eof == true and will return -1, and any threads entering skip() will
    * find eof == true and return 0.
    */
      
      log.debug("entering handleRemoteShutdown()");
      
      if (eof)
         return;
      
      remoteShutDownPending = true;
      
      if (available() == 0)
      {  
         setEOF();
         interruptReadingThreads();
      }
      
      log.debug("leaving handleRemoteShutdown()");
   }


/**
 *
 */
   protected synchronized void interruptReadingThreads()
   {  
      // If we obtained the lock, then either there are no threads in read() or skip(),
      // or any such threads are blocked in super.read(), having executed wait().
      Iterator it = readingThreads.iterator();
      
      while (it.hasNext())
      {
         Thread t = (Thread) it.next();
         it.remove();
         t.interrupt();
      }
   }
   
   
/**
 * <code>readInt()</code> is borrowed from DataInputStream.  It saves the extra expense of
 * creating a DataInputStream
 */
   public final int readInt() throws IOException
   {
      int b1 = read();
      int b2 = read();
      int b3 = read();
      int b4 = read();
      
      if ((b1 | b2 | b3 | b4) < 0)
          throw new EOFException();
      
      return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
  }
   
   
/**
 */
   protected void setEOF()
   {
      eof = true;
   }
   
   
   protected void setReadException(IOException e)
   {
      readException = e;
      interruptReadingThreads();
   }
   
/**
 * @param n
 */
   protected synchronized void setSkip(long n)
   {
      skipCount += n;
   }
   
   
/**
 * A MultiplexingInputStream may be created without reference to a VirtualSocket.  
 * (See MultiplexingManager.getAnOutputStream().)  setSocket() allows the socket to 
 * be set afterwards.
 * 
 * @param socket
 */
   protected void setSocket(VirtualSocket socket)
   {
      this.socket = socket;
   }
}