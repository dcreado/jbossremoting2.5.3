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

package org.jboss.remoting.marshal.compress;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.serialization.SerializationStreamFactory;


/**
 * <code>CompressingMarshaller</code> and <code>CompressingUnMarshaller</code> are a general
 * purpose compressing marshaller / decompressing unmarshaller pair based on Java's GZIP facilities.
 * <p/>
 * <code>CompressingMarshaller</code> is subclassed from <code>SerializableMarshaller</code>, and by default
 * it uses <code>super.write()</code> to marshall an object, which is then
 * compressed.  Optionally, it can wrap any other marshaller and use that instead of
 * <code>SerializableMarshaller</code> to marshall an object before it is compressed.  For example,
 * <p/>
 * <center><code>new CompressingMarshaller(new HTTPMarshaller())</code></center>
 * <p/>
 * will create a marshaller that compresses the output of an <code>HTTPMarshaller</code>.
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 5324 $
 *          <p/>
 *          Copyright (c) 2005
 *          </p>
 */

public class CompressingMarshaller extends SerializableMarshaller
{
   public final static String DATATYPE = "compressible";

   private Marshaller wrappedMarshaller;
   private static final long serialVersionUID = 8731343309128430753L;

   /**
    * Create a new CompressingMarshaller.
    */
   public CompressingMarshaller()
   {
   }


   /**
    * Create a new CompressingMarshaller.
    *
    * @param marshaller A <code>Marshaller</code> which is used to turn objects into byte streams.
    */
   public CompressingMarshaller(Marshaller marshaller)
   {
      wrappedMarshaller = marshaller;
   }

   public OutputStream getMarshallingStream(OutputStream outputStream) throws IOException
   {
      GZIPOutputStream gzos = new SelfCleaningGZipOutputStream(outputStream);
      DecomposableBufferedOutputStream bos = new DecomposableBufferedOutputStream(gzos);
      return bos;
   }
   

   /**
    * Writes compressed, marshalled form of <code>dataObject</code> to <code>output</code>.
    *
    * @param dataObject arbitrary object to be marshalled
    * @param output     <code>OutputStream</code> to which <code>output</code> is to be marshalled
    * @param version    wire format version
    */
   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      SelfCleaningGZipOutputStream gzos = null;
      DecomposableBufferedOutputStream bos = null;
      if (output instanceof DecomposableBufferedOutputStream)
      {
         bos = (DecomposableBufferedOutputStream) output;
         gzos = (SelfCleaningGZipOutputStream) bos.getWrappedStream();
         gzos.refreshDeflater();
      }
      else
      {
         gzos = new SelfCleaningGZipOutputStream(output);
         bos = new DecomposableBufferedOutputStream(gzos);
      }
      ObjectOutputStream oos = SerializationStreamFactory.getManagerInstance(getSerializationType()).createOutput(bos);

      if(wrappedMarshaller != null)
      {
         if (wrappedMarshaller instanceof VersionedMarshaller)
            ((VersionedMarshaller) wrappedMarshaller).write(dataObject, oos, version);
         else
            wrappedMarshaller.write(dataObject, oos);
      }
      else
      {
         super.write(dataObject, oos, version);
      }

      oos.flush();
      bos.flush();
      gzos.finish();
      gzos.flush();

   }

   /**
    * Returns a <code>CompressingMarshaller</code>.
    *
    * @return a <code>CompressingMarshaller</code>.
    * @throws CloneNotSupportedException In practice no exceptions are thrown
    */
   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      return new CompressingMarshaller(wrappedMarshaller);
   }
   
   
   /**
    * @author Doychin Bondzhev
    */
   static class SelfCleaningGZipOutputStream extends GZIPOutputStream
   {
      boolean used;
      
      public SelfCleaningGZipOutputStream(OutputStream out) throws IOException
      {
         super(out);
      }
      
      void refreshDeflater()
      {
         if (used)
         {
            def = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            crc.reset();
         }
         else
         {
            used = true;
         }
      }

      /**
       * Writes remaining compressed data to the output stream and closes the underlying stream.
       * @exception IOException if an I/O error has occurred
       */
      public void finish() throws IOException
      {
         super.finish();
         def.end(); // This will release all resources used by zlib native code
      }
   }
   
   static class DecomposableBufferedOutputStream extends BufferedOutputStream
   {
      DecomposableBufferedOutputStream(OutputStream out, int size)
      {
         super(out, size);
      }

      DecomposableBufferedOutputStream(OutputStream out)
      {
         super(out);
      }
      
      OutputStream getWrappedStream()
      {
         return out;
      }
   }
}

