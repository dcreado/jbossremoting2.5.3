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
package org.jboss.remoting.marshal.encryption;

import java.io.Serializable;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;

//$Id: EncryptionUtil.java 1398 2006-08-25 21:09:03Z asaldhana $

/**
 *  Utility for Encryption Needs
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 25, 2006 
 *  @version $Revision: 1398 $
 */
public class EncryptionUtil
{
   private static String algo = EncryptionManager.TRIPLEDES;
   
   public static Serializable unsealObject(SealedObject so)
   throws Exception
   {
      Cipher ci = EncryptionManager.getCipher(Cipher.DECRYPT_MODE, algo);
      Serializable ser = (Serializable)so.getObject(ci);
      return ser; 
   }
   
   public static SealedObject sealObject(Serializable ser)
   throws Exception
   {
      Cipher ci = EncryptionManager.getCipher(Cipher.ENCRYPT_MODE, algo);
      return new SealedObject(ser,ci); 
   }
}
