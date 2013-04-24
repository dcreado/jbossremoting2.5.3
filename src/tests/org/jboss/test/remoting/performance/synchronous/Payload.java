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

package org.jboss.test.remoting.performance.synchronous;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Payload implements Externalizable
{
   private int callNumber = 0;
   private Object payload = null;

   public Payload()
   {

   }

   public Payload(Object payload)
   {
      this.payload = payload;
   }

   public void setCallNumber(int callNumber)
   {
      this.callNumber = callNumber;
   }

   public int getCallNumber()
   {
      return callNumber;
   }

   public Object getPayload()
   {
      return payload;
   }

   public String toString()
   {
      StringBuffer buffer = new StringBuffer("Payload (" + super.toString() + ") contains:\n");
      buffer.append("call number: " + callNumber + "\n");
      buffer.append("payload value: " + payload + "\n");
      return buffer.toString();
   }

   /**
    * The object implements the readExternal method to restore its
    * contents by calling the methods of DataInput for primitive
    * types and readObject for objects, strings and arrays.  The
    * readExternal method must read the values in the same sequence
    * and with the same types as were written by writeExternal.
    *
    * @param in the stream to read data from in order to restore the object
    * @throws java.io.IOException    if I/O errors occur
    * @throws ClassNotFoundException If the class for an object being
    *                                restored cannot be found.
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      //System.out.println("read new version");
      callNumber = in.readInt();
      int size = in.readInt();
      byte[] bytes = new byte[size];

      int n = 0;
      while (n < size)
      {
         n += in.read(bytes, n, size - n);
      }
      this.payload = bytes;
   }

   /**
    * The object implements the writeExternal method to save its contents
    * by calling the methods of DataOutput for its primitive values or
    * calling the writeObject method of ObjectOutput for objects, strings,
    * and arrays.
    *
    * @param out the stream to write the object to
    * @throws java.io.IOException Includes any I/O exceptions that may occur
    * @serialData Overriding methods should use this tag to describe
    * the data layout of this Externalizable object.
    * List the sequence of element types and, if possible,
    * relate the element to a public/protected field and/or
    * method of this Externalizable class.
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      //System.out.println("write new version");
      out.writeInt(callNumber);
      byte[] bytes = (byte[]) payload;
      out.writeInt(bytes.length);
      out.write(bytes, 0, bytes.length);
   }
}