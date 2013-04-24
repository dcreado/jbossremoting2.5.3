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


package org.jboss.remoting.serialization.impl.jboss;

import org.jboss.logging.Logger;
import org.jboss.remoting.loading.ObjectInputStreamWithClassLoader;
import org.jboss.remoting.serialization.IMarshalledValue;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.serial.io.JBossObjectOutputStream;
import org.jboss.serial.util.StringUtilBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Instantiates the Streamings according to JbossObjectOutputStream and JBossObjectInputStream.
 * Also, it uses a different approach for MarshallValues as we don't need to convert objects in bytes.
 * $Id: JBossSerializationManager.java 3976 2008-04-12 04:32:06Z ron.sigal@jboss.com $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public class JBossSerializationManager extends SerializationManager
{
   protected static final Logger log = Logger.getLogger(JBossSerializationManager.class);

   private static boolean trace = log.isTraceEnabled();

   public ObjectInputStream createInput(final InputStream input, final ClassLoader loader) throws IOException
   {
      if (trace) { log.trace(this + " creating JBossObjectInputStream"); }
      
      if (SecurityUtility.skipAccessControl())
      {
         return new JBossObjectInputStream(input, loader, new StringUtilBuffer(10024, 10024));
      }
      
      try
      {
         return (ObjectInputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new JBossObjectInputStream(input, loader, new StringUtilBuffer(10024, 10024));
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   public ObjectOutputStream createOutput(final OutputStream output) throws IOException
   {
      if (trace) { log.trace(this + " creating JBossObjectOutputStream"); }

      if (SecurityUtility.skipAccessControl())
      {
         return new JBossObjectOutputStream(output, new StringUtilBuffer(10024, 10024));
      }
      
      try
      {
         return (ObjectOutputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new JBossObjectOutputStream(output, new StringUtilBuffer(10024, 10024));
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   /**
    * Creates a MarshalledValue that does lazy serialization.
    */
   public IMarshalledValue createdMarshalledValue(Object source) throws IOException
   {
      if (source instanceof IMarshalledValue)
      {
         return (IMarshalledValue) source;
      }
      else
      {
         return new MarshalledValue(source);
      }
   }

   public IMarshalledValue createMarshalledValueForClone(Object original) throws IOException
   {
      return new SmartCloningMarshalledValue(original);
   }


   public void sendObject(final ObjectOutputStream oos, final Object dataObject, int version) throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         oos.writeObject(dataObject);
         oos.flush();
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               oos.writeObject(dataObject);
               oos.flush();
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }

   public Object receiveObject(InputStream inputStream, ClassLoader customClassLoader, int version)
   throws IOException, ClassNotFoundException
   {
      ObjectInputStream objInputStream = null;
      Object obj = null;
      if (inputStream instanceof ObjectInputStreamWithClassLoader)
      {
         ((ObjectInputStreamWithClassLoader) inputStream).setClassLoader(customClassLoader);
         objInputStream = (ObjectInputStream) inputStream;
      }
      else if (inputStream instanceof JBossObjectInputStream)
      {
         ((JBossObjectInputStream) inputStream).setClassLoader(customClassLoader);
         objInputStream = (ObjectInputStream) inputStream;
      }
      else if (inputStream instanceof ObjectInputStream)
      {
         objInputStream = (ObjectInputStream) inputStream;
      }
      else
      {
         if (customClassLoader != null)
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JBOSS).createInput(inputStream, customClassLoader);
         }
         else
         {
            objInputStream = SerializationStreamFactory.getManagerInstance(SerializationStreamFactory.JBOSS).createRegularInput(inputStream);
         }
      }

      if (SecurityUtility.skipAccessControl())
      {
         return objInputStream.readObject();
      }
      
      try
      {
         final ObjectInputStream ois = objInputStream;
         obj = AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return ois.readObject();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }

      return obj;
   }

   public String toString()
   {
      return "JBossSerializationManager[" + Integer.toHexString(hashCode()) + "]";
   }

}
