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
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.crypto.SealedObject;

import org.jboss.remoting.marshal.encryption.EncryptionUtil;

//$Id: JavaEncryptionSerializationManager.java 2412 2007-05-19 02:25:47Z rsigal $

/**
 *  Seals/Unseals Serialized Objects
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 25, 2006 
 *  @version $Revision: 2412 $
 */
public class JavaEncryptionSerializationManager 
extends JavaSerializationManager
{ 
   public Object receiveObject(InputStream inputStream, ClassLoader customClassLoader, int version)
   throws IOException, ClassNotFoundException
   {
      Object obj = super.receiveObject(inputStream, customClassLoader, version);
      if(obj instanceof SealedObject)
      {
         try
         {
            obj = EncryptionUtil.unsealObject((SealedObject)obj);
         }
         catch (Exception e)
         { 
            e.printStackTrace();
         }
      }
      return obj;
   }
 
   public void sendObject(ObjectOutputStream oos, Object dataObject, int version) throws IOException
   {
      if(dataObject instanceof Serializable)
      try
      {
        dataObject = EncryptionUtil.sealObject((Serializable)dataObject);
      }
      catch (Exception e)
      { 
         e.printStackTrace();
      } 
      super.sendObject(oos, dataObject, version);
   } 
}
