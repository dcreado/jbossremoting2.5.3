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

package org.jboss.remoting.stream;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is the server side proxy back to the orginal stream
 * on the client side.  It implements InputStream, so can be
 * passed and acted on by the server handler as a regular InputStream
 * type.  For all the InputStream methods, it should behave EXACTLY
 * like a local InputStream with the one exception being that it
 * will sometimes throw IOExceptions based on network exceptions
 * or in the case when the method does not throw an IOException, throwing
 * a RuntimeException if network problem (however none of the method
 * signatures are changed).
 * <p/>
 * Internally, it will use remoting to callback to the client.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class StreamHandler extends InputStream //implements InvocationHandler
{
   private InvokerLocator streamServerLocator = null;
   private Client streamClient = null;

   private static final Logger log = Logger.getLogger(StreamHandler.class);

   // The remoting invocation methods the match the InputStream metnhods.
   public static final String READ = "read()";
   public static final String AVAILABLE = "available()";
   public static final String CLOSE = "close()";
   public static final String RESET = "reset()";
   public static final String MARKSUPPORTED = "markSupported()";
   public static final String MARKREADLIMIT = "mark(int readlimit)";
   public static final String SKIP = "skip(long n)";
   public static final String READBYTEARRAY = "read(byte b[])";
   public static final String READOFFSET = "read(byte b[], int off, int len)";

   /**
    * Constructor requiring the locator url back to the client's
    * StreamServer connector (actually the connector's server invoker).
    *
    * @param locatorURL
    * @throws Exception
    */
   //private StreamHandler(String locatorURL) throws Exception
   public StreamHandler(String locatorURL) throws Exception
   {
      streamServerLocator = new InvokerLocator(locatorURL);
      streamClient = new Client(streamServerLocator);
      streamClient.connect();
   }

   /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without blocking by the next caller of a method for
    * this input stream.  The next caller might be the same thread or or
    * another thread.
    * <p/>
    * <p> The <code>available</code> method for class <code>InputStream</code>
    * always returns <code>0</code>.
    * <p/>
    * <p> This method should be overridden by subclasses.
    *
    * @return the number of bytes that can be read from this input stream
    *         without blocking.
    * @throws java.io.IOException if an I/O error occurs.
    */
   public int available() throws IOException
   {
      int readInt = 0;

      try
      {
         Integer retInt = (Integer) streamClient.invoke(new StreamCallPayload(AVAILABLE));
         if(retInt != null)
         {
            readInt = retInt.intValue();
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error getting available from client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }
      return readInt;
   }

   /**
    * Closes this input stream and releases any system resources associated
    * with the stream.
    * <p/>
    * <p> The <code>close</code> method of <code>InputStream</code> does
    * nothing.
    *
    * @throws java.io.IOException if an I/O error occurs.
    */
   public void close() throws IOException
   {
      try
      {
         streamClient.invoke(new StreamCallPayload(CLOSE));
      }
      catch(Throwable throwable)
      {
         log.debug("Error closing client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }
   }

   /**
    * Repositions this stream to the position at the time the
    * <code>mark</code> method was last called on this input stream.
    * <p/>
    * <p> The general contract of <code>reset</code> is:
    * <p/>
    * <p><ul>
    * <p/>
    * <li> If the method <code>markSupported</code> returns
    * <code>true</code>, then:
    * <p/>
    * <ul><li> If the method <code>mark</code> has not been called since
    * the stream was created, or the number of bytes read from the stream
    * since <code>mark</code> was last called is larger than the argument
    * to <code>mark</code> at that last call, then an
    * <code>IOException</code> might be thrown.
    * <p/>
    * <li> If such an <code>IOException</code> is not thrown, then the
    * stream is reset to a state such that all the bytes read since the
    * most recent call to <code>mark</code> (or since the start of the
    * file, if <code>mark</code> has not been called) will be resupplied
    * to subsequent callers of the <code>read</code> method, followed by
    * any bytes that otherwise would have been the next input data as of
    * the time of the call to <code>reset</code>. </ul>
    * <p/>
    * <li> If the method <code>markSupported</code> returns
    * <code>false</code>, then:
    * <p/>
    * <ul><li> The call to <code>reset</code> may throw an
    * <code>IOException</code>.
    * <p/>
    * <li> If an <code>IOException</code> is not thrown, then the stream
    * is reset to a fixed state that depends on the particular type of the
    * input stream and how it was created. The bytes that will be supplied
    * to subsequent callers of the <code>read</code> method depend on the
    * particular type of the input stream. </ul></ul>
    * <p/>
    * <p> The method <code>reset</code> for class <code>InputStream</code>
    * does nothing and always throws an <code>IOException</code>.
    *
    * @throws java.io.IOException if this stream has not been marked or if the
    *                             mark has been invalidated.
    * @see java.io.InputStream#mark(int)
    * @see java.io.IOException
    */
   public synchronized void reset() throws IOException
   {
      try
      {
         streamClient.invoke(new StreamCallPayload(RESET));
      }
      catch(Throwable throwable)
      {
         log.debug("Error reseting client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }
   }

   /**
    * Tests if this input stream supports the <code>mark</code> and
    * <code>reset</code> methods. Whether or not <code>mark</code> and
    * <code>reset</code> are supported is an invariant property of a
    * particular input stream instance. The <code>markSupported</code> method
    * of <code>InputStream</code> returns <code>false</code>.
    *
    * @return <code>true</code> if this stream instance supports the mark
    *         and reset methods; <code>false</code> otherwise.
    * @see java.io.InputStream#mark(int)
    * @see java.io.InputStream#reset()
    */
   public boolean markSupported()
   {
      boolean supported = false;

      try
      {
         Boolean bSupported = (Boolean) streamClient.invoke(new StreamCallPayload(MARKSUPPORTED));
         if(bSupported != null)
         {
            supported = bSupported.booleanValue();
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error getting markSupported from client stream.", throwable);
         throw new RuntimeException(throwable.getMessage(), throwable);
      }
      return supported;
   }

   /**
    * Marks the current position in this input stream. A subsequent call to
    * the <code>reset</code> method repositions this stream at the last marked
    * position so that subsequent reads re-read the same bytes.
    * <p/>
    * <p> The <code>readlimit</code> arguments tells this input stream to
    * allow that many bytes to be read before the mark position gets
    * invalidated.
    * <p/>
    * <p> The general contract of <code>mark</code> is that, if the method
    * <code>markSupported</code> returns <code>true</code>, the stream somehow
    * remembers all the bytes read after the call to <code>mark</code> and
    * stands ready to supply those same bytes again if and whenever the method
    * <code>reset</code> is called.  However, the stream is not required to
    * remember any data at all if more than <code>readlimit</code> bytes are
    * read from the stream before <code>reset</code> is called.
    * <p/>
    * <p> The <code>mark</code> method of <code>InputStream</code> does
    * nothing.
    *
    * @param readlimit the maximum limit of bytes that can be read before
    *                  the mark position becomes invalid.
    * @see java.io.InputStream#reset()
    */
   public synchronized void mark(int readlimit)
   {
      try
      {
         StreamCallPayload payload = new StreamCallPayload(MARKREADLIMIT);
         payload.setParams(new Object[]{new Integer(readlimit)});
         streamClient.invoke(payload);
      }
      catch(Throwable throwable)
      {
         log.debug("Error marking with read limit on client stream.", throwable);
         throw new RuntimeException(throwable.getMessage(), throwable);
      }
   }

   /**
    * Skips over and discards <code>n</code> bytes of data from this input
    * stream. The <code>skip</code> method may, for a variety of reasons, end
    * up skipping over some smaller number of bytes, possibly <code>0</code>.
    * This may result from any of a number of conditions; reaching end of file
    * before <code>n</code> bytes have been skipped is only one possibility.
    * The actual number of bytes skipped is returned.  If <code>n</code> is
    * negative, no bytes are skipped.
    * <p/>
    * <p> The <code>skip</code> method of <code>InputStream</code> creates a
    * byte array and then repeatedly reads into it until <code>n</code> bytes
    * have been read or the end of the stream has been reached. Subclasses are
    * encouraged to provide a more efficient implementation of this method.
    *
    * @param n the number of bytes to be skipped.
    * @return the actual number of bytes skipped.
    * @throws java.io.IOException if an I/O error occurs.
    */
   public long skip(long n) throws IOException
   {
      long numSkipped = -1;

      try
      {
         StreamCallPayload payload = new StreamCallPayload(SKIP);
         payload.setParams(new Object[]{new Long(n)});
         Long ret = (Long) streamClient.invoke(payload);
         if(ret != null)
         {
            numSkipped = ret.longValue();
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error skipping on client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }

      return numSkipped;
   }

   /**
    * Reads some number of bytes from the input stream and stores them into
    * the buffer array <code>b</code>. The number of bytes actually read is
    * returned as an integer.  This method blocks until input data is
    * available, end of file is detected, or an exception is thrown.
    * <p/>
    * <p> If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown.  If the length of
    * <code>b</code> is zero, then no bytes are read and <code>0</code> is
    * returned; otherwise, there is an attempt to read at least one byte. If
    * no byte is available because the stream is at end of file, the value
    * <code>-1</code> is returned; otherwise, at least one byte is read and
    * stored into <code>b</code>.
    * <p/>
    * <p> The first byte read is stored into element <code>b[0]</code>, the
    * next one into <code>b[1]</code>, and so on. The number of bytes read is,
    * at most, equal to the length of <code>b</code>. Let <i>k</i> be the
    * number of bytes actually read; these bytes will be stored in elements
    * <code>b[0]</code> through <code>b[</code><i>k</i><code>-1]</code>,
    * leaving elements <code>b[</code><i>k</i><code>]</code> through
    * <code>b[b.length-1]</code> unaffected.
    * <p/>
    * <p> If the first byte cannot be read for any reason other than end of
    * file, then an <code>IOException</code> is thrown. In particular, an
    * <code>IOException</code> is thrown if the input stream has been closed.
    * <p/>
    * <p> The <code>read(b)</code> method for class <code>InputStream</code>
    * has the same effect as: <pre><code> read(b, 0, b.length) </code></pre>
    *
    * @param b the buffer into which the data is read.
    * @return the total number of bytes read into the buffer, or
    *         <code>-1</code> is there is no more data because the end of
    *         the stream has been reached.
    * @throws java.io.IOException  if an I/O error occurs.
    * @throws NullPointerException if <code>b</code> is <code>null</code>.
    * @see java.io.InputStream#read(byte[], int, int)
    */
   public int read(byte b[]) throws IOException
   {
      if(b == null)
      {
         throw new NullPointerException("can not read for a null byte array.");
      }
      else
      {
         if(b.length == 0)
         {
            return 0;
         }
      }

      int retByte = -1;

      try
      {
         StreamCallPayload payload = new StreamCallPayload(READBYTEARRAY);
         payload.setParams(new Object[]{b});
         StreamCallPayload ret = (StreamCallPayload) streamClient.invoke(payload);
         if(ret != null)
         {
            Object[] retVals = ret.getParams();
            byte[] retBytes = (byte[]) retVals[0];
            Integer retInt = (Integer) retVals[1];

            retByte = retInt.intValue();

            if(retByte != -1)
            {
               System.arraycopy(retBytes, 0, b, 0, retByte);
            }
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error reading from client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }

      return retByte;
   }

   /**
    * Reads up to <code>len</code> bytes of data from the input stream into
    * an array of bytes.  An attempt is made to read as many as
    * <code>len</code> bytes, but a smaller number may be read, possibly
    * zero. The number of bytes actually read is returned as an integer.
    * <p/>
    * <p> This method blocks until input data is available, end of file is
    * detected, or an exception is thrown.
    * <p/>
    * <p> If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown.
    * <p/>
    * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
    * thrown.
    * <p/>
    * <p> If <code>len</code> is zero, then no bytes are read and
    * <code>0</code> is returned; otherwise, there is an attempt to read at
    * least one byte. If no byte is available because the stream is at end of
    * file, the value <code>-1</code> is returned; otherwise, at least one
    * byte is read and stored into <code>b</code>.
    * <p/>
    * <p> The first byte read is stored into element <code>b[off]</code>, the
    * next one into <code>b[off+1]</code>, and so on. The number of bytes read
    * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
    * bytes actually read; these bytes will be stored in elements
    * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
    * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
    * <code>b[off+len-1]</code> unaffected.
    * <p/>
    * <p> In every case, elements <code>b[0]</code> through
    * <code>b[off]</code> and elements <code>b[off+len]</code> through
    * <code>b[b.length-1]</code> are unaffected.
    * <p/>
    * <p> If the first byte cannot be read for any reason other than end of
    * file, then an <code>IOException</code> is thrown. In particular, an
    * <code>IOException</code> is thrown if the input stream has been closed.
    * <p/>
    * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method
    * for class <code>InputStream</code> simply calls the method
    * <code>read()</code> repeatedly. If the first such call results in an
    * <code>IOException</code>, that exception is returned from the call to
    * the <code>read(b,</code> <code>off,</code> <code>len)</code> method.  If
    * any subsequent call to <code>read()</code> results in a
    * <code>IOException</code>, the exception is caught and treated as if it
    * were end of file; the bytes read up to that point are stored into
    * <code>b</code> and the number of bytes read before the exception
    * occurred is returned.  Subclasses are encouraged to provide a more
    * efficient implementation of this method.
    *
    * @param b   the buffer into which the data is read.
    * @param off the start offset in array <code>b</code>
    *            at which the data is written.
    * @param len the maximum number of bytes to read.
    * @return the total number of bytes read into the buffer, or
    *         <code>-1</code> if there is no more data because the end of
    *         the stream has been reached.
    * @throws java.io.IOException  if an I/O error occurs.
    * @throws NullPointerException if <code>b</code> is <code>null</code>.
    * @see java.io.InputStream#read()
    */
   public int read(byte b[], int off, int len) throws IOException
   {
      if(b == null)
      {
         throw new NullPointerException("can not read for a null byte array.");
      }
      else
      {
         if(b.length == 0)
         {
            return 0;
         }
         else
         {
            if(off < 0 || len < 0 || off + len > b.length)
            {
               throw new IndexOutOfBoundsException("Either off or len is negative or off+len is greater than length of b.");
            }
            if(len == 0)
            {
               return 0;
            }
         }
      }

      int retByte = -1;

      try
      {
         byte[] payloadArray = new byte[len];
         StreamCallPayload payload = new StreamCallPayload(READBYTEARRAY);
         payload.setParams(new Object[]{payloadArray});
         StreamCallPayload ret = (StreamCallPayload) streamClient.invoke(payload);
         if(ret != null)
         {
            Object[] retVals = ret.getParams();
            byte[] retBytes = (byte[]) retVals[0];
            Integer retInt = (Integer) retVals[1];

            retByte = retInt.intValue();

            if(retByte != -1)
            {
               System.arraycopy(retBytes, 0, b, off, retByte);
            }
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error reading with offset from client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }

      return retByte;
   }

   /**
    * Reads the next byte of data from the input stream. The value byte is
    * returned as an <code>int</code> in the range <code>0</code> to
    * <code>255</code>. If no byte is available because the end of the stream
    * has been reached, the value <code>-1</code> is returned. This method
    * blocks until input data is available, the end of the stream is detected,
    * or an exception is thrown.
    * <p/>
    * <p> A subclass must provide an implementation of this method.
    *
    * @return the next byte of data, or <code>-1</code> if the end of the
    *         stream is reached.
    * @throws java.io.IOException if an I/O error occurs.
    */
   public int read() throws IOException
   {
      int readInt = -1;

      try
      {
         Integer retInt = (Integer) streamClient.invoke(new StreamCallPayload(READ));
         if(retInt != null)
         {
            readInt = retInt.intValue();
         }
      }
      catch(Throwable throwable)
      {
         log.debug("Error reading from client stream.", throwable);
         throw new IOException(throwable.getMessage());
      }
      return readInt;
   }

}