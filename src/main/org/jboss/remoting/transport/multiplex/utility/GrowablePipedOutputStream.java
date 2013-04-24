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
import java.io.OutputStream;



/**
 * <code>GrowablePipedOutputStream</code> works together with
 * <code>GrowablePipedInputStream</code> like <code>java.io.PipedInputStream</code>
 * and <code>java.io.PipedOutputStream</code> work together, so that
 * calling <code>GrowablePipedOutputStream.write()</code> causes bytes to be deposited with the
 * matching <code>GrowablePipedInputStream</code>.  However, unlike
 * <code>PipedInputStream</code>, <code>GrowablePipedInputStream</code> stores
 * bytes in a <code>ShrinkableByteArrayOutputStream</code>, which
 * can grow and contract dynamically in response to the number of bytes it contains.
 *
 * <p>
 * For more information about method behavior, see the <code>java.io.OutputStream</code> javadoc.
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public class GrowablePipedOutputStream extends OutputStream
{
   private GrowablePipedInputStream sink;
   private boolean connected;

/**
 * Create a new <code>GrowablePipedOutputStream</code>.
 * 
 * 
 */
   public GrowablePipedOutputStream()
   {
   }
 
   
/**
 * Create a new <code>GrowablePipedOutputStream</code>.
 * 
 * @param snk
 * @throws java.io.IOException
 */
   public GrowablePipedOutputStream(GrowablePipedInputStream sink) throws IOException
   {
      this.sink = sink;
      sink.connect(this);
      connected = true;
   }

   
   public void write(int b) throws IOException
   {
      if (sink == null)
         throw new IOException("Pipe not connected");

      sink.receive(b);
   }

   
   public void write(byte[] bytes) throws IOException
   {
      if (sink == null)
         throw new IOException("Pipe not connected");
      
      if (bytes == null)
 	    throw new NullPointerException();
      
      sink.receive(bytes);
   }
   
   
   public void write(byte[] bytes, int offset, int length) throws IOException
   {
      if (sink == null)
         throw new IOException("Pipe not connected");
      
      if (bytes == null)
 	    throw new NullPointerException();
      
      if ((offset < 0) || (offset > bytes.length) || (length < 0) ||
 		   ((offset + length) > bytes.length) || ((offset + length) < 0))
         throw new IndexOutOfBoundsException("offset = " + offset + 
                                             ", length = " + length +
                                             ", file buffer size: " + bytes.length);
      
      if (length == 0)
         return;
      
      sink.receive(bytes, offset, length);
   }
   
   
   protected void connect(GrowablePipedInputStream sink) throws IOException
   {
      if (sink == null)
         throw new NullPointerException();
      
      if (sink.isConnected())
         throw new IOException("Already connected");
      
      this.sink = sink;
      connected = true;
   }
   
   
   protected boolean isConnected()
   {
      return connected;
   }
}

