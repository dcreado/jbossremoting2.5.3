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
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.Key;
 
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream; 

import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;


/**
 * <code>EncryptingMarshaller</code> and <code>EncryptingMarshaller</code> are a general
 * purpose encryption based marshaller / decompressing unmarshaller pair 
 * based on Java's Cipher facilities.
 * <p/>
 * <code>EncryptingMarshaller</code> is subclassed from <code>SerializableMarshaller</code>, 
 * and by default it uses <code>super.write()</code> to marshall an object, which is then
 * encrypted.  Optionally, it can wrap any other marshaller and use that instead of
 * <code>SerializableMarshaller</code> to marshall an object before it is encrypted.
 * For example,
 * <p/>
 * <center><code>new EncryptingMarshaller(new HTTPMarshaller())</code></center>
 * <p/>
 * will create a marshaller that encrypts the output of an <code>HTTPMarshaller</code>.
 *
 * @author <a href="mailto:anil.saldhana@jboss.com">Anil Saldhana</a> 
 */

public class EncryptingMarshaller extends SerializableMarshaller
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 1L;

   public final static String DATATYPE = "encrypt";

   private Marshaller wrappedMarshaller; 
   
   private String cipherAlgorithm = EncryptionManager.DEFAULT_CIPHER_ALGORITHM;
   
   private Cipher cipher = EncryptionManager.getCipher(Cipher.ENCRYPT_MODE, cipherAlgorithm);

   /**
    * Create a new EncryptingMarshaller.
    */
   public EncryptingMarshaller()
   { 
   }
   
   /**
    * 
    * Create a new EncryptingMarshaller.
    * 
    * @param algo Cipher Algorithm
    * @param key Key
    * @see #setCipherAlgorithm(String)
    */
   public EncryptingMarshaller(String algo, Key key)
   { 
      cipher = EncryptionManager.getCipher(Cipher.ENCRYPT_MODE, algo, key);
   }


   /**
    * Create a new EncryptingMarshaller.
    *
    * @param marshaller A <code>Marshaller</code> which is used to turn objects into byte streams.
    */
   public EncryptingMarshaller(Marshaller marshaller)
   {
      wrappedMarshaller = marshaller;
   }
   
   /**
    * Set the Cipher Algorithm to use
    * @param algo
    * @see EncryptionManager#DEFAULT_CIPHER_ALGORITHM
    */
   public void setCipherAlgorithm(String algo)
   {
      this.cipherAlgorithm = algo;
      cipher = EncryptionManager.getCipher(Cipher.ENCRYPT_MODE, this.cipherAlgorithm);
   }

   public OutputStream getMarshallingStream(OutputStream outputStream) throws IOException
   {
      return outputStream;
   }
   
   /**
    * Writes encrypted, marshalled form of <code>dataObject</code> to <code>output</code>.
    *
    * @param dataObject arbitrary object to be marshalled
    * @param output     <code>OutputStream</code> to which <code>output</code> is to be marshalled
    * @param version    wire format version
    */
   public void write(Object dataObject, OutputStream output, int version) throws IOException
   { 
      if(cipher == null)
         throw new IllegalStateException("Cipher is null for algo="+ this.cipherAlgorithm);
      output.flush(); 
      
      //EOS intercepts the close() call and does not close the stream
      EncryptionOutputStream eos = new EncryptionOutputStream(output);
       
      CipherOutputStream cos = new CipherOutputStream(eos, cipher);
       
      SerializationManager sm = SerializationStreamFactory.getManagerInstance(getSerializationType());
      ObjectOutputStream oos = sm.createOutput(cos);
      
      if(wrappedMarshaller != null)
      {
         if (wrappedMarshaller instanceof VersionedMarshaller)
            ((VersionedMarshaller) wrappedMarshaller).write(dataObject, oos, version);
         else
            wrappedMarshaller.write(dataObject, oos);
      }
      else
      {
         super.write(dataObject, oos, version);
      }  
      oos.flush();  
      
      //Vagaries of CipherOutputStream which needs a close() to flush at the end
      cos.close(); //Tests fail without this statement - oos.close() should do it
      oos.close(); //There is a need to close cos
   }

   /**
    * Returns a <code>EncryptingMarshaller</code>.
    *
    * @return a <code>EncryptingMarshaller</code>.
    * @throws CloneNotSupportedException In practice no exceptions are thrown
    */
   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      EncryptingMarshaller em = new EncryptingMarshaller(wrappedMarshaller);
      em.setCipherAlgorithm(this.cipherAlgorithm);
      return em;
   } 
}

