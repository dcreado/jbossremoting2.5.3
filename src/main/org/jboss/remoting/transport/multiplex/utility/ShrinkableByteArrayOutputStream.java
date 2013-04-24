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
 * Created on Oct 15, 2005
 */
 
package org.jboss.remoting.transport.multiplex.utility;

import java.io.ByteArrayOutputStream;



/**
 * ShrinkableByteArrayOutputStream extends java.io.ByteArrayOutputStream and adds
 * the following features:
 * <p>
 * <ol>
 *  <li>Rather than creating a new byte array with each call to <code>toByteArray()</code>,
 *   it returns a reference to its internal byte array.  <code>start()</code>
 *   returns the position of the next available byte and <code>available()</code>
 *   returns the number of bytes of content, starting at <code>start()</code>, are available.
 *  <li>It reuses its capacity, treating its byte array as a circular queue.  When
 *   <code>write()</code> is called, if there is too little space at the end of the buffer,
 *   and less than half of the capacity is currently in use, it will shift the current
 *   contents of its buffer to position 0.
 *  <li>When <code>toByteArray()</code> is called, if less than a quarter of the
 *   current capacity is in use and the current capacity is greater than <code>MIN_LENGTH</code>
 *   (currently defined as 1024), it will copy the current contents to a byte array
 *   one half the size of the current byte array.
 *  </ol>
 *  
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public class ShrinkableByteArrayOutputStream extends ByteArrayOutputStream
{
   static private int MIN_LENGTH = 1024;
   private int used = 0;
   private int nextUsed = 0;
   private int bytesReturned = 0;
   
   
   /**
    * Create a new ShrinkableByteArrayOutputStream.
    */
   public ShrinkableByteArrayOutputStream()
   {
      super(MIN_LENGTH);
   }
   
   
   /**
    * Create a new ShrinkableByteArrayOutputStream.
    */
   public ShrinkableByteArrayOutputStream(int size)
   {
      super(size);
   }
   
   
   /**
    * Returns number of bytes of content which can be retrieved.
    * @return number of bytes of content which can be retrieved
    */
   public int available()
   {
      return count - nextUsed;
   }
   
   
   /**
    * Returns number of bytes of content returned by last call to <code>toByteArray()</code>.
    * @return number of bytes of content returned by last call to <code>toByteArray()</code>
    */
   public int bytesReturned()
   {
      return bytesReturned;
   }
   
   
   /**
    * Returns  position of next available byte of content in byte array returned
    *         by <code>toByteArray()</code>.
    * @return position of next available byte of content in byte array returned
    *         by <code>toByteArray()</code>
    */
   public int start()
   {
      return used;
   }
   
   
   /**
    * Returns reference to internal byte array.
    * @param length number of bytes desired
    * @return reference to internal byte array
    */
   public byte[] toByteArray(int length)
   {
      used = nextUsed;
      int currentLength = buf.length;
      int quarterLength = currentLength >> 2;
      
      if (currentLength > MIN_LENGTH && currentLength - used <= quarterLength)
      {
         byte newbuf[] = new byte[currentLength >> 1];
         System.arraycopy(buf, used, newbuf, 0, available());
         buf = newbuf;
         count -= used;
         used = 0;
      }
      
      bytesReturned = Math.min(count - used, length);
      nextUsed = used + bytesReturned;
      return buf;
   }

   
   /**
    * Writes the specified byte to this byte array output stream. 
    *
    * @param   b   the byte to be written.
    */
   public void write(int b) 
   {
      if (count == buf.length && used > buf.length >> 1)
      {
         System.arraycopy(buf, used, buf, 0, count - used);
         nextUsed -= used;
         count -= used;
         used = 0;
      }
      
      super.write(b);
   }
   
   
   /**
    * Writes <code>len</code> bytes from the specified byte array 
    * starting at offset <code>off</code> to this byte array output stream.
    *
    * @param   b     the data.
    * @param   off   the start offset in the data.
    * @param   len   the number of bytes to write.
    */
   public void write(byte b[], int off, int len)
   {
       if (count + len > buf.length && used > buf.length >> 1)
       {
          System.arraycopy(buf, used, buf, 0, count - used);
          nextUsed -= used;
          count -= used;
          used = 0;
       }
       
       super.write(b, off, len);
   }
   
   
   protected int getCount()
   {
      return super.size();
   }
   
   
   protected int getLength()
   {
      return buf.length;
   }
   
   
   protected int getNextUsed()
   {
      return nextUsed;
   }
   
   
   protected int getUsed()
   {
      return used;
   }

}

