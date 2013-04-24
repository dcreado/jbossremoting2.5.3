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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream; 
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;


/**
 * <code>EncryptingMarshaller</code> and <code>EncryptingUnMarshaller</code> are a general
 * purpose encrypting marshaller / decompressing unmarshaller pair based on 
 * Java's crypto stream facilities.
 * <p/>
 * <code>EncryptingUnMarshaller</code> is subclassed from <code>SerializableUnMarshaller</code>,
 * and by default it uses <code>super.read()</code> to deserialize an object, once the object has been
 * decrypted.  Optionally, it can wrap any other unmarshaller and use that instead of
 * <code>SerializableUnMarshaller</code> to unmarshall an encrypted input stream.  For example,
 * <p/>
 * <center><code>new EncryptingUnMarshaller(new HTTPUnMarshaller())</code></center
 * <p/>
 * will create an umarshaller that
 * uses an <code>HTTPUnMarshaller</code> to restore an unencrypted input stream.
 *
 * @author Anil.Saldhana@jboss.org
 * @version $Revision: 3584 $ 
 */

public class EncryptingUnMarshaller extends SerializableUnMarshaller
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   public final static String DATATYPE = "encrypt";

   private UnMarshaller wrappedUnMarshaller; 
   
   private String cipherAlgorithm = EncryptionManager.DEFAULT_CIPHER_ALGORITHM;
   
   private Cipher cipher = EncryptionManager.getCipher(Cipher.DECRYPT_MODE, cipherAlgorithm);
   

   /**
    * Create a new EncryptingUnMarshaller.
    */
   public EncryptingUnMarshaller()
   {
   }
 

   /**
    * Create a new EncryptingUnMarshaller.
    *
    * @param unMarshaller unmarshaller to be used to restore 
    * unencrypted byte stream to original object
    */
   public EncryptingUnMarshaller(UnMarshaller unMarshaller)
   {
      wrappedUnMarshaller = unMarshaller;
   }

   /**
    * Set the Cipher Algorithm to use
    * @param algo
    * @see EncryptionManager#DEFAULT_CIPHER_ALGORITHM
    */
   public void setCipherAlgorithm(String algo)
   {
      this.cipherAlgorithm = algo;
      cipher = EncryptionManager.getCipher(Cipher.DECRYPT_MODE, this.cipherAlgorithm);
   }

   public InputStream getMarshallingStream(InputStream inputStream) throws IOException
   {
      return inputStream;
   }
   
   /**
    * Restores a encrypted, marshalled form of an object to its original state.
    *
    * @param inputStream <code>InputStream</code> from which marshalled form is to be retrieved
    * @param metadata    can be any transport specific metadata (such as headers from http transport).
    *                    This can be null, depending on if transport supports metadata.
    * @param version     wire format version
    * @return restored object
    * @throws IOException            if there is a problem reading from <code>inputStream</code>
    * @throws ClassNotFoundException if there is a problem finding a class needed for unmarshalling
    */
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {  
      if(cipher == null)
         throw new IllegalStateException("Cipher is null for algo="+ this.cipherAlgorithm);
      CipherInputStream cis = new CipherInputStream(inputStream,cipher);
      SerializationManager sm = SerializationStreamFactory.getManagerInstance(getSerializationType());
      ObjectInputStream ois = sm.createRegularInput(cis);
       
      Object obj = null;
      if(wrappedUnMarshaller != null)
      {
         if (wrappedUnMarshaller instanceof VersionedUnMarshaller)
            return ((VersionedUnMarshaller)wrappedUnMarshaller).read(ois, metadata, version);
         else
            obj = wrappedUnMarshaller.read(ois, metadata); 
      }
      else
      {
         obj = super.read(ois, metadata, version);
      } 
      return obj;
   } 

   /**
    * Returns a new <code>EncryptingUnMarshaller</code>
    *
    * @return a new <code>EncryptingUnMarshaller</code>
    * @throws CloneNotSupportedException In practice no exceptions are thrown.
    */
   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      EncryptingUnMarshaller um = new EncryptingUnMarshaller(wrappedUnMarshaller);
      um.setCipherAlgorithm(this.cipherAlgorithm);
      return um;
   } 
}

