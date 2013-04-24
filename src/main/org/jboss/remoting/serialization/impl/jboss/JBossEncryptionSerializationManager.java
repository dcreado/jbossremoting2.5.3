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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.crypto.SealedObject;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.marshal.encryption.EncryptionUtil;

//$Id: JBossEncryptionSerializationManager.java 1399 2006-08-25 21:10:57Z asaldhana $

/**
 *  Encrypted version of JBoss Serialization Manager
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 25, 2006 
 *  @version $Revision: 1399 $
 */
public class JBossEncryptionSerializationManager extends JBossSerializationManager
{ 
   public Object receiveObject(InputStream inputStream, ClassLoader customClassLoader) throws IOException, ClassNotFoundException
   {
      Object object = super.receiveObject(inputStream, customClassLoader);
      if(object instanceof InvocationResponse)
      {
         InvocationResponse ir = (InvocationResponse)object;
         Object obj = ir.getResult();
         if(obj instanceof SealedObject)
         {
            try
            {
               object = new InvocationResponse(ir.getSessionId(), 
                     (EncryptionUtil.unsealObject((SealedObject)obj)),
                               ir.isException(), ir.getPayload()); 
            }
            catch (Exception e)
            { 
               e.printStackTrace();
            }
         }
      }
      return object;
   }
 
   public void sendObject(ObjectOutputStream oos, Object dataObject) throws IOException
   { 
      if(dataObject instanceof InvocationRequest)
      {
         InvocationRequest ir = (InvocationRequest)dataObject;
         Object obj = ir.getParameter();
         if(obj instanceof Serializable)
         {
            try
            {
               ir.setParameter(EncryptionUtil.sealObject((Serializable)obj));
            }
            catch (Exception e)
            { 
               e.printStackTrace();
            }
         }
      } 
      super.sendObject(oos, dataObject);
   } 
}
