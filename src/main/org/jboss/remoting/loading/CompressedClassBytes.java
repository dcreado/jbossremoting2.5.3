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
package org.jboss.remoting.loading;

import java.io.Externalizable;
import java.io.StreamCorruptedException;
import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * CompressedClassBytes is a ClassBytes subclass that compresses
 * class data, if possible.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 3835 $
 */
public class CompressedClassBytes extends ClassBytes implements Externalizable
{
   static final long serialVersionUID = 5984363018051268886L;

   private static final boolean DEBUG = 
      ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(Boolean.getBoolean("jboss.remoting.compression.debug"));
         }
      })).booleanValue();
   
   private static final int MIN_COMPRESS = 
      ((Integer)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Integer.getInteger("jboss.remoting.compression.min", 1000);
         }
      })).intValue();
   
   private int compressionLevel;
   private int compressedSize;
   private int originalSize;

   static final int VERSION_5_0 = 500;
   static final int CURRENT_VERSION = VERSION_5_0;

   public CompressedClassBytes()
   {
      super(null, null);
   }

   public CompressedClassBytes(String className, byte data[], int compressionLevel)
   {
      super(className, data);
      this.compressionLevel = compressionLevel;
   }

   public static void main(String args[])
   {
      try
      {
         String string = new String("Hello,world - this is a test of compression, not sure what will happen. alskjfdalksjflkajsdfljaslkfjaslkdjflksajflkajsfdlkjsalkfjaslkfdjlksajflkasjfdlkajslkfjsalkfjasldfjlksadjflkasjfdlkajdsf");
         byte buf [] = org.jboss.remoting.loading.ClassUtil.serialize(string);
         CompressedClassBytes cb = new CompressedClassBytes("java.lang.String", buf, 9);
         byte b1[] = org.jboss.remoting.loading.ClassUtil.serialize(cb);
         ClassLoader classLoader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return ClassLoader.getSystemClassLoader();
            }
         });
         Object obj = ClassUtil.deserialize(b1, classLoader);
      }
      catch (Throwable ex)
      {
         ex.printStackTrace();
      }
   }

   public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException
   {
      int version = in.readInt();

      switch (version)
      {

         case VERSION_5_0:
         {

            compressionLevel = in.readInt();
            originalSize = in.readInt();
            compressedSize = in.readInt();
            byte buf[] = new byte[compressedSize];
            int count = in.read(buf, 0, compressedSize);
            if (compressedSize != originalSize)
            {
               this.classBytes = uncompress(buf);
            }
            else
            {
               this.classBytes = buf;
            }
            if (DEBUG)
            {
               System.err.println("<< reading compressed: " + compressedSize + ", original: " + originalSize + ", compressionLevel:" + compressionLevel);
            }
            this.className = (String) in.readObject();
            break;
         }
         default:
            throw new StreamCorruptedException("Unknown version seen: " + version);
      }
   }

   public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException
   {
      out.writeInt(CURRENT_VERSION);
      out.writeInt(compressionLevel);
      out.writeInt(classBytes.length);
      byte compressed [] = compress(classBytes);
      out.writeInt(compressed.length);
      out.write(compressed);
      out.writeObject(className);
      out.flush();
   }


   /**
    * Compresses the input data.
    *
    * @return null if compression results in larger output.
    */
   public byte[] compress(byte[] input)
   {
      // Too small to spend time compressing
      if (input.length < MIN_COMPRESS)
      {
         return input;
      }

      java.util.zip.Deflater deflater = new java.util.zip.Deflater(compressionLevel);
      deflater.setInput(input, 0, input.length);
      deflater.finish();
      byte[] buff = new byte[input.length + 50];
      deflater.deflate(buff);

      int compressedSize = deflater.getTotalOut();

      // Did this data compress well?
      if (deflater.getTotalIn() != input.length)
      {
         if (DEBUG)
         {
            System.err.println(">> Attempting compression and the data didn't compress well, returning original");
         }
         return input;
      }
      if (compressedSize >= input.length - 4)
      {
         if (DEBUG)
         {
            System.err.println(">> Compressed size is larger than original .. ?");
         }
         return input;
      }

      byte[] output = new byte[compressedSize + 4];
      System.arraycopy(buff, 0, output, 4, compressedSize);
      output[0] = (byte) (input.length >> 24);
      output[1] = (byte) (input.length >> 16);
      output[2] = (byte) (input.length >> 8);
      output[3] = (byte) (input.length);
      if (DEBUG)
      {
         System.err.println(">> writing compressed: " + output.length + ", original: " + classBytes.length + ", compressionLevel:" + compressionLevel);
      }
      return output;
   }

   /**
    * Un-compresses the input data.
    *
    * @throws java.io.IOException if the input is not valid.
    */
   public byte[] uncompress(byte[] input) throws java.io.IOException
   {
      try
      {
         int uncompressedSize =
               (((input[0] & 0xff) << 24) +
                ((input[1] & 0xff) << 16) +
                ((input[2] & 0xff) << 8) +
                ((input[3] & 0xff)));

         java.util.zip.Inflater inflater = new java.util.zip.Inflater();
         inflater.setInput(input, 4, input.length - 4);
         inflater.finished();

         byte[] out = new byte[uncompressedSize];
         inflater.inflate(out);

         inflater.reset();
         return out;

      }
      catch (java.util.zip.DataFormatException e)
      {
         throw new java.io.IOException("Input Stream is corrupt: " + e);
      }
   }
}
