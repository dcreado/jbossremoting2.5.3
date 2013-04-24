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

package org.jboss.remoting.marshal.serializable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;

import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.PreferredStreamMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;

/**
 * Simple marshaller that simply serializes java objects
 * using standard output stream.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SerializableMarshaller implements PreferredStreamMarshaller, VersionedMarshaller
{
   static final long serialVersionUID = -5553685435323600244L;

   public final static String DATATYPE = "serializable";

   String serializationType;


   public void setSerializationType(String serializationType)
   {
      this.serializationType = serializationType;
   }

   public String getSerializationType()
   {
      if(serializationType == null)
      {
         return "java";
      }
      else
      {
         return serializationType;
      }
   }
   
   public OutputStream getMarshallingStream(OutputStream outputStream) throws IOException
   {
      return getMarshallingStream(outputStream, null);
   }
   
   /**
    * SerializableMarshaller prefers to write to an ObjectOutputStream wrapped around a
    * BufferedOutputStream.
    */
   public OutputStream getMarshallingStream(OutputStream outputStream, Map config) throws IOException
   {
      if(outputStream instanceof ObjectOutputStream)
      {
         return outputStream;
      }
      else
      {
         BufferedOutputStream bos = new BufferedOutputStream(outputStream);
         SerializationManager manager = SerializationStreamFactory.getManagerInstance(getSerializationType());
         ObjectOutputStream oos = manager.createOutput(bos);
         oos.flush();
         return oos;
      }
   }

   /**
    * Take the data object and write to the output.  Has ben customized
    * for working with ObjectOutputStreams since requires extra messaging.
    *
    * @param dataObject Object to be writen to output
    * @param output     The data output to write the object
    *                   data to.
    */
   public void write(Object dataObject, OutputStream output) throws IOException
   {
      int version = Version.getDefaultVersion();
      write(dataObject, output, version);
   }

   /**
    * Take the data object and write to the output.  Has ben customized
    * for working with ObjectOutputStreams since requires extra messaging.
    *
    * @param dataObject Object to be writen to output
    * @param output     The data output to write the object data to.
    * @param version    Wire format version
    */
   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      ObjectOutputStream oos = (ObjectOutputStream) getMarshallingStream(output, null);
      SerializationStreamFactory.getManagerInstance(getSerializationType()).sendObject(oos, dataObject, version);
   }
   
   public Marshaller cloneMarshaller()
         throws CloneNotSupportedException
   {
      return new SerializableMarshaller();
   }
}