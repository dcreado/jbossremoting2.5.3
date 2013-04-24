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
 * Created on Dec 15, 2005
 */
 
package org.jboss.remoting.transport.multiplex.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import org.jboss.logging.Logger;


/**
 * <code>GrowablePipedInputStream</code> is the parent of the
 * <code>MultiplexingInputStream</code> returned by
 * <code>VirtualSocket.getInputStream()</code>.  <code>GrowablePipedInputStream</code> and
 * <code>GrowablePipedOutputStream</code> work together like <code>java.io.PipedInputStream</code>
 * and <code>java.io.PipedOutputStream</code>, so that
 * calling <code>GrowablePipedOutputStream.write()</code> causes bytes to be deposited with the
 * matching <code>GrowablePipedInputStream</code>.  However, unlike <code>PipedInputStream</code>,
 * <code>GrowablePipedInputStream</code> stores bytes in a 
 * <code>ShrinkableByteArrayOutputStream</code>, which
 * can grow and contract dynamically in response to the number of bytes it contains.
 *
 * <p>
 * For more information about method behavior, see the <code>java.io.InputStream</code> javadoc.
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public class GrowablePipedInputStream extends InputStream
{
   protected static final Logger log = Logger.getLogger(GrowablePipedInputStream.class);
   private GrowablePipedOutputStream source;
   private ShrinkableByteArrayOutputStream baos = new ShrinkableByteArrayOutputStream();
   private VirtualSelector virtualSelector;
   private boolean connected;
   private int timeout;

   
/**
    * Create a new <code>GrowablePipedInputStream</code>.
    */
   public GrowablePipedInputStream()
   {
   }
   
/**
 * Create a new <code>GrowablePipedInputStream</code>.
 * @param virtualSelector 
 */
   public GrowablePipedInputStream(VirtualSelector virtualSelector)
   {
      this.virtualSelector = virtualSelector;
   }


/**
 * Create a new <code>GrowablePipedInputStream</code>.
 * @param src
 * 
 * @throws java.io.IOException
 */
   public GrowablePipedInputStream(GrowablePipedOutputStream source) throws IOException
   {
      this.source = source;
      source.connect(this);
      connected = true;
   }
   
      
/**
 * Create a new <code>GrowablePipedInputStream</code>.
 * @param virtualSelector 
 * @param src
 * 
 * @throws java.io.IOException
 */
   public GrowablePipedInputStream(GrowablePipedOutputStream source, VirtualSelector virtualSelector) throws IOException
   {
      this.source = source;
      this.virtualSelector = virtualSelector;
      source.connect(this);
      connected = true;
   }
   

   public synchronized int available()
   {
      return baos.available();
   }
   
   
   public void close() throws IOException
   {
      super.close();
      if (virtualSelector != null)
         virtualSelector.unregister(this);
   }
   
   
   public int getTimeout()
   {
      return timeout;
   }
   
   
   public synchronized int read() throws IOException
   {
      if (!connected)
         throw new IOException("Pipe not connected");
      
      if (baos.available() == 0)
      {
         long start = System.currentTimeMillis();
         
         while (true)
         {  
            try
            {
               log.trace(this + ": entering wait()");
               wait(timeout);
               log.trace("leaving wait()");
               
               if (baos.available() > 0)
                  break;
               
               if (0 < timeout && timeout <= System.currentTimeMillis() - start)
                  throw new SocketTimeoutException("Read timed out");
            }
            catch (InterruptedException ignored)
            {
               log.debug("interrupted");
               throw new InterruptedIOException();
            }
         }
      }
      
      byte[] bytes = baos.toByteArray(1);
      int answer =  0xff & bytes[baos.start()];
      
      if (baos.available() > 0)
         notify();
      
      return answer;
   }
   
   
   public synchronized int read(byte[] bytes) throws IOException
   {
      return read(bytes, 0, bytes.length);
   }
   
   
   public synchronized int read(byte[] bytes, int offset, int length) throws IOException
   {
      if (!connected)
         throw new IOException("Pipe not connected");

      if (baos.available() == 0)
      {
         long start = System.currentTimeMillis();
         
         while (true)
         {  
            try
            {
               log.trace(this + ": entering wait()");
               wait(timeout);
               log.trace("leaving wait()");
               
               if (baos.available() > 0)
                  break;
               
               if (0 < timeout && timeout <= System.currentTimeMillis() - start)
                  throw new SocketTimeoutException("Read timed out");
            }
            catch (InterruptedException ignored)
            {
               log.debug("interrupted");
               throw new InterruptedIOException();
            }
         }
      }
      
      byte[] localBytes = baos.toByteArray(length);
      int from = baos.start();
      int n = baos.bytesReturned();
      System.arraycopy(localBytes, from, bytes, offset, n);
      
      if (baos.available() > 0)
         notify();
      
      return n;
   }
   
   
   public void register(VirtualSelector virtualSelector, Object attachment)
   {
      this.virtualSelector = virtualSelector;
      virtualSelector.register(this, attachment);
   }
   
   
   public void setTimeout(int timeout)
   {
      this.timeout = timeout;
   }
   
   
   protected void connect(GrowablePipedOutputStream source) throws IOException
   {
      if (source == null)
         throw new NullPointerException();
      
      if (source.isConnected())
         throw new IOException("Already connected");
      
      this.source = source;
      connected = true;
   }
   
   
   protected boolean isConnected()
   {
      return connected;
   }
   
   
   protected void receive(int i) throws IOException
   {
      log.trace("entering receive()");
      synchronized (this)
      {
         baos.write(i);
         notify();
      }
      
      if (virtualSelector != null)
         virtualSelector.addToReadyInputStreams(this);
   }
   
   
   protected void receive(byte[] bytes) throws IOException
   {
      log.trace("entering receive()");
      synchronized (this)
      {
         baos.write(bytes);
         notify();
      }
      
      if (virtualSelector != null)
         virtualSelector.addToReadyInputStreams(this);
   }
   
   
   protected void receive(byte[] bytes, int offset, int length) throws IOException
   {
      log.trace(this + ": entering receive()");
      synchronized (this)
      {
         baos.write(bytes, offset, length);
         log.trace(this + ": notifying");
         notify();
      }
      
      if (virtualSelector != null)
         virtualSelector.addToReadyInputStreams(this);
   }
}