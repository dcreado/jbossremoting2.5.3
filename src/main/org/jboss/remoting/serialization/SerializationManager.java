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


package org.jboss.remoting.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.remoting.Version;

/**
 * Controls the creation of ObjectInputStream, ObjectOutputStream.
 * <p/>
 * It is important that implementation of this class needs to be stateless.
 * $Id: SerializationManager.java 3843 2008-04-02 04:10:43Z ron.sigal@jboss.com $
 *
 * @author <a href="mailto:tclebert.suconic@jboss.com">Clebert Suconic</a>
 */
public abstract class SerializationManager
{
   public ObjectInputStream createRegularInput(InputStream input) throws IOException
   {
      ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
      return createInput(input, tcl);
   }

   public abstract ObjectInputStream createInput(InputStream input, ClassLoader loader) throws IOException;

   public abstract ObjectOutputStream createOutput(OutputStream output) throws IOException;

   /**
    * Creates a MarshalledValue that does lazy serialization.
    */
   public abstract IMarshalledValue createdMarshalledValue(Object source) throws IOException;

   public void sendObject(ObjectOutputStream output, Object dataObject) throws IOException
   {
      int version = Version.getDefaultVersion();
      sendObject(output, dataObject, version);
   }
   
   public abstract void sendObject(ObjectOutputStream output, Object dataObject, int version) throws IOException;

   /** Used in call by value operations.
    * This will use the most effective way */
	public abstract IMarshalledValue createMarshalledValueForClone(Object original) throws IOException;

   /**
    * This was a refactory of a method usually existent on {@link org.jboss.remoting.marshal.serializable.SerializableUnMarshaller}.
    * That's why we are using InputStream instead of ObjectInputStream as a parameter here.
    */
   public Object receiveObject(InputStream input, ClassLoader customClassLoader) throws IOException, ClassNotFoundException
   {
      int version = Version.getDefaultVersion();
      return receiveObject(input, customClassLoader, version);
   }
   
   public abstract Object receiveObject(InputStream input, ClassLoader customClassLoader, int version) throws IOException, ClassNotFoundException;

}
