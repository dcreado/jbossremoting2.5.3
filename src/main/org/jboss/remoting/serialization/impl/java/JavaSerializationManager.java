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


package org.jboss.remoting.serialization.impl.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import org.jboss.logging.Logger;
import org.jboss.remoting.Version;
import org.jboss.remoting.loading.ObjectInputStreamWithClassLoader;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.serialization.IMarshalledValue;

/**
 * $Id: JavaSerializationManager.java 4519 2008-08-30 06:20:07Z ron.sigal@jboss.com $
 *
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class JavaSerializationManager extends SerializationManager
{
   protected static final Logger log = Logger.getLogger(JavaSerializationManager.class);

   public ObjectInputStream createInput(InputStream input, ClassLoader loader) throws IOException
   {
      if(log.isTraceEnabled())
      {
         log.trace("Creating ObjectInputStreamWithClassLoader");
      }
      return new ObjectInputStreamWithClassLoader(input, loader);
   }

   public ObjectOutputStream createOutput(OutputStream output) throws IOException
   {
      if(log.isTraceEnabled())
      {
         log.trace("Creating ObjectOutputStream");
      }
      return new ClearableObjectOutputStream(output); 
   }

   /**
    * Creates a MarshalledValue that does lazy serialization.
    */
   public IMarshalledValue createdMarshalledValue(Object source) throws IOException
   {
      if(source instanceof IMarshalledValue)
      {
         return (IMarshalledValue) source;
      }
      else
      {
         return new JavaMarshalledValue(source);
      }
   }

    public IMarshalledValue createMarshalledValueForClone(Object original) throws IOException {
        return createdMarshalledValue(original);
    }
   
   public void sendObject(ObjectOutputStream oos, Object dataObject, int version) throws IOException
   {
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
            sendObjectVersion1_2(oos, dataObject);
            break;
            
         case Version.VERSION_2_2:
            sendObjectVersion2_2(oos, dataObject);
            break;
            
         default:
            throw new IOException("Can not process version " + version + ". " +
                  "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);

      }
   }
   
   protected void sendObjectVersion1_2(ObjectOutputStream oos, Object dataObject) throws IOException
   {
      oos.writeObject(dataObject);
      oos.reset();
      // to make sure stream gets reset
      // Stupid ObjectInputStream holds object graph
      // can only be set by the client/server sending a TC_RESET
      oos.writeObject(Boolean.TRUE);
      oos.flush();
      oos.reset();
   }
   
   protected void sendObjectVersion2_2(ObjectOutputStream oos, Object dataObject) throws IOException
   {
      oos.reset();
      oos.writeObject(dataObject);
      oos.flush();
      
      if (oos instanceof ClearableObjectOutputStream)
      {
         ((ClearableObjectOutputStream) oos).clear();
      }
   }
   
   public Object receiveObject(InputStream inputStream, ClassLoader customClassLoader, int version) throws IOException, ClassNotFoundException
   {
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
            return receiveObjectVersion1_2(inputStream, customClassLoader);
            
         case Version.VERSION_2_2:
            return receiveObjectVersion2_2(inputStream, customClassLoader);
            
         default:
            throw new IOException("Can not process version " + version + ". " +
                  "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);

      }
   }
   
   protected Object receiveObjectVersion1_2(InputStream inputStream, ClassLoader customClassLoader) throws IOException, ClassNotFoundException
   {
      ObjectInputStream objInputStream = null;
      Object obj = null;
      if(inputStream instanceof ObjectInputStreamWithClassLoader)
      {
         ((ObjectInputStreamWithClassLoader) inputStream).setClassLoader(customClassLoader);
         objInputStream = (ObjectInputStream) inputStream;
      }
      /*else if(inputStream instanceof JBossObjectInputStream)
 {
     if(((JBossObjectInputStream) inputStream).getClassLoader() == null)
     {
        ((JBossObjectInputStream) inputStream).setClassLoader(customClassLoader);
     }
     objInputStream = (ObjectInputStream) inputStream;
 } -- for future reference */
      else if(inputStream instanceof ObjectInputStream)
      {
         objInputStream = (ObjectInputStream) inputStream;
      }
      else
      {
         if(customClassLoader != null)
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JAVA).createInput(inputStream, customClassLoader);
         }
         else
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JAVA).createRegularInput(inputStream);
         }
      }

      try
      {
      obj = objInputStream.readObject();
      }
      catch (IOException e)
      {
         log.debug("", e);
         throw e;
      }

      try
      {
         objInputStream.readObject(); // for stupid ObjectInputStream reset
      }
      catch(Exception e)
      {
         /**
          * Putting try catch around this because if using servlet sever invoker, the previous
          * call is not needed, so will throw EOFException and can ignore.
          */
      }
      return obj;
   }
   
   protected Object receiveObjectVersion2_2(InputStream inputStream, ClassLoader customClassLoader) throws IOException, ClassNotFoundException
   {
      ObjectInputStream objInputStream = null;
      Object obj = null;
      if(inputStream instanceof ObjectInputStreamWithClassLoader)
      {
         ((ObjectInputStreamWithClassLoader) inputStream).setClassLoader(customClassLoader);
         objInputStream = (ObjectInputStream) inputStream;
      }
      /*else if(inputStream instanceof JBossObjectInputStream)
 {
     if(((JBossObjectInputStream) inputStream).getClassLoader() == null)
     {
        ((JBossObjectInputStream) inputStream).setClassLoader(customClassLoader);
     }
     objInputStream = (ObjectInputStream) inputStream;
 } -- for future reference */
      else if(inputStream instanceof ObjectInputStream)
      {
         objInputStream = (ObjectInputStream) inputStream;
      }
      else
      {
         if(customClassLoader != null)
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JAVA).createInput(inputStream, customClassLoader);
         }
         else
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JAVA).createRegularInput(inputStream);
         }
      }


      obj = objInputStream.readObject();

      if(inputStream instanceof ObjectInputStreamWithClassLoader)
      {
         ((ObjectInputStreamWithClassLoader) inputStream).clearCache();
      }
      
      return obj;
   }
}