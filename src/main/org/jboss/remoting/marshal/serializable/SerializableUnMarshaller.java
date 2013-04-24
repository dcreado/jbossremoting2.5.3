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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.jboss.remoting.Version;
import org.jboss.remoting.marshal.PreferredStreamUnMarshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.UpdateableClassloaderUnMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;

/**
 * Will perform the deserialization of objects off the wire.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SerializableUnMarshaller
implements PreferredStreamUnMarshaller, VersionedUnMarshaller, UpdateableClassloaderUnMarshaller
{
   static final long serialVersionUID = -1554017376768780738L;

   public final static String DATATYPE = "serializable";

   protected ClassLoader customClassLoader;

   protected String serializationType;
   
   public InputStream getMarshallingStream(InputStream inputStream) throws IOException
   {
      return getMarshallingStream(inputStream, null);
   }
   
   /**
    * SerializableUnMarshaller prefers to read from an ObjectOutputStream wrapped around a 
    * BufferedInputStream
    */
   public InputStream getMarshallingStream(InputStream inputStream, Map config) throws IOException
   {
      if (inputStream instanceof ObjectInputStream)
      {
         return inputStream;
      }
      else
      {
         BufferedInputStream bis = new BufferedInputStream(inputStream);
         SerializationManager manager = SerializationStreamFactory.getManagerInstance(getSerializationType());
         return manager.createInput(bis, getClassLoader());
      }
      
   }


   /**
    * Reads the data from the input stream and converts to an Object.
    * <p/>
    * If the inputStream passed is an ObjectInputStream (which would prefer it not be), it will just
    * use it as given.  If the input stream is not an instance of ObjectInputStream, will wrap it with a
    * custom ObjectInputStream (ObjectInputStreamWithClassLoader) and use it.  The ObjectInputStreamWithClassLoader
    * will use the custom class loader set in order to ensure that any classes not found within the local classloader
    * can be loaded from the server and used within the client VM.  If the inpustream is of type ObjectInputStreamWithClassLoader,
    * then will just set the classloader to the custom classloader and proceed.<p>
    *
    * @param inputStream
    * @param metadata
    * @return
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public Object read(InputStream inputStream, Map metadata) throws IOException, ClassNotFoundException
   {
      int version = Version.getDefaultVersion();
      return read(inputStream, metadata, version);
   }

   /**
    * Reads the data from the input stream and converts to an Object.
    * <p/>
    * If the inputStream passed is an ObjectInputStream (which would prefer it not be), it will just
    * use it as given.  If the input stream is not an instance of ObjectInputStream, will wrap it with a
    * custom ObjectInputStream (ObjectInputStreamWithClassLoader) and use it.  The ObjectInputStreamWithClassLoader
    * will use the custom class loader set in order to ensure that any classes not found within the local classloader
    * can be loaded from the server and used within the client VM.  If the inpustream is of type ObjectInputStreamWithClassLoader,
    * then will just set the classloader to the custom classloader and proceed.<p>
    *
    * @param inputStream
    * @param metadata
    * @param version
    * @return
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {
      ObjectInputStream ois = (ObjectInputStream) getMarshallingStream(inputStream, null);
      return SerializationStreamFactory.getManagerInstance(getSerializationType()).receiveObject(ois, getClassLoader(), version);

   }
   
   /**
    * Sets the classloader to be used when deserializing objects off the wire.  This will ONLY be used in the
    * when the input stream passed to the read() method is NOT an instance of ObjectInputStream.<p>
    *
    * @param classloader
    */
   public void setClassLoader(ClassLoader classloader)
   {
      this.customClassLoader = classloader;
   }
   
   public ClassLoader getClassLoader()
   {
      return customClassLoader;
   }

   public UnMarshaller cloneUnMarshaller()
         throws CloneNotSupportedException
   {
      SerializableUnMarshaller unmarshaller = new SerializableUnMarshaller();
      unmarshaller.setClassLoader(getClassLoader());
      return unmarshaller;
   }

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
}