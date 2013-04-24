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

package org.jboss.remoting.marshal.http;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.serialization.SerializationStreamFactory;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPMarshaller extends SerializableMarshaller
{
   static final long serialVersionUID = -51801299849879386L;

   public final static String DATATYPE = "http";

   /**
    * Take the data object and write to the output.
    * If the data object is of type String, will just
    * write the bytes of the String, otherwise will
    * use the SerializableMarshaller implementation.
    *
    * @param dataObject Object to be writen to output
    * @param output     The data output to write the object
    *                   data to.
    * @param version    Wire format version.
    */
   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      if(dataObject instanceof String)
      {
         output.write(((String) dataObject).getBytes());
         output.flush();
      }
      else
      {
//         ObjectOutputStream oos = null;
//         if(output instanceof ObjectOutputStream)
//         {
//            oos = (ObjectOutputStream) output;
//         }
//         else
//         {
//            oos = SerializationStreamFactory.getManagerInstance(getSerializationType()).createOutput(output);
//         }
//         oos.writeObject(dataObject);
//
//         oos.flush();
         super.write(dataObject, output, version);
      }
   }

   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      return new HTTPMarshaller();
   }
}