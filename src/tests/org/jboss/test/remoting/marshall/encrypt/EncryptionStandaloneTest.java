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
package org.jboss.test.remoting.marshall.encrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.encryption.EncryptingMarshaller;
import org.jboss.remoting.marshal.encryption.EncryptingUnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

import junit.framework.TestCase;

//$Id: EncryptionStandaloneTest.java 3560 2008-03-02 06:22:43Z ron.sigal@jboss.com $

/**
 *  Tests Remoting Encryption facilities
 *  @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 *  @since  Aug 16, 2006 
 *  @version $Revision: 3560 $
 */
public class EncryptionStandaloneTest extends TestCase
{
   private Marshaller marshaller;
   private UnMarshaller unmarshaller;
   
   private String[] standard = new String[]
        {null, "AES", "DES", "Blowfish", "DESede"};
   
   private String[] padded = new String[]
        {"AES/CBC/PKCS5Padding", "AES/PCBC/PKCS5Padding","AES/PCBC/PKCS5Padding",
         "AES/CFB/PKCS5Padding", "AES/OFB/PKCS5Padding",
         "DES/CBC/PKCS5Padding", "DES/PCBC/PKCS5Padding","DES/PCBC/PKCS5Padding",
         "DES/CFB/PKCS5Padding", "DES/OFB/PKCS5Padding",
         "DESede/CBC/PKCS5Padding", "DESede/PCBC/PKCS5Padding","DESede/PCBC/PKCS5Padding",
         "DESede/CFB/PKCS5Padding", "DESede/OFB/PKCS5Padding"};
   
   private String[] unpadded = new String[]
        {"AES/CBC/NoPadding", "AES/PCBC/NoPadding","AES/PCBC/NoPadding",
         "AES/CFB/NoPadding", "AES/OFB/NoPadding",
         "DES/CBC/NoPadding", "DES/PCBC/NoPadding","DES/PCBC/NoPadding",
         "DES/CFB/NoPadding", "DES/OFB/NoPadding",
         "DESede/CBC/NoPadding", "DESede/PCBC/NoPadding","DESede/PCBC/NoPadding",
         "DESede/CFB/NoPadding", "DESede/OFB/NoPadding"}; 

   
   public void testSerializable() throws IOException, ClassNotFoundException
   {  
      for(int i = 0 ; i < standard.length; i++)
         runAlgoTest(standard[i]);
      for(int i = 0 ; i < padded.length; i++)
         runAlgoTest(padded[i]);
      for(int i = 0 ; i < unpadded.length; i++)
         runAlgoTest(unpadded[i]); 
   }


   public void testWrappedSerializable() throws IOException, ClassNotFoundException
   { 
      for(int i = 0 ; i < standard.length; i++)
         runWrappedSerializableTest(standard[i]);
      for(int i = 0 ; i < padded.length; i++)
         runWrappedSerializableTest(padded[i]);
      for(int i = 0 ; i < unpadded.length; i++)
         runWrappedSerializableTest(unpadded[i]);  
   }

   /**
    * Commented out usecases not supported
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public void testHTTP() throws IOException, ClassNotFoundException
   { 
      for(int i = 0 ; i < standard.length; i++)
         runHttpTest(standard[i]);
      for(int i = 0 ; i < padded.length; i++)
         runHttpTest(padded[i]);
      //NoPadding is not correctly supported by HttpUnMarshaller
      /*for(int i = 0 ; i < unpadded.length; i++)
         runHttpTest(unpadded[i]); */  
   }
   

   protected void runOneTest() throws IOException, ClassNotFoundException
   {
      String testData = "This is some test data";
      Object param = new String(testData);

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      marshaller.write(param, output);
      byte[] byteArray = new byte[output.size()];
      byteArray = output.toByteArray();
      ByteArrayInputStream input = new ByteArrayInputStream(byteArray);
      Object result = unmarshaller.read(input, null);

      System.out.println("Result: " + result);
      assertEquals(testData, result);
   }
   
   private void runAlgoTest(String algo) 
   throws IOException, ClassNotFoundException
   {
      EncryptingMarshaller em = new EncryptingMarshaller();
      EncryptingUnMarshaller um = new EncryptingUnMarshaller();
      if(algo != null)
      {
         em.setCipherAlgorithm(algo);
         um.setCipherAlgorithm(algo);
      }
      MarshalFactory.addMarshaller(EncryptingMarshaller.DATATYPE,em, um); 

      marshaller = MarshalFactory.getMarshaller(EncryptingMarshaller.DATATYPE);
      unmarshaller = MarshalFactory.getUnMarshaller(EncryptingMarshaller.DATATYPE);
      runOneTest(); 
   }
   
   private void runHttpTest(String algo) 
   throws IOException, ClassNotFoundException
   {
      String datatype = "encryptedHTTP"; 
      Marshaller m = MarshalFactory.getMarshaller(HTTPMarshaller.DATATYPE);
      UnMarshaller u = MarshalFactory.getUnMarshaller(HTTPUnMarshaller.DATATYPE);
      EncryptingMarshaller em =  new EncryptingMarshaller(m);
      EncryptingUnMarshaller um =  new EncryptingUnMarshaller(u);
      if(algo != null)
      {
         em.setCipherAlgorithm(algo);
         um.setCipherAlgorithm(algo);
      }
      MarshalFactory.addMarshaller(datatype,em, um); 
      marshaller = MarshalFactory.getMarshaller(datatype);
      unmarshaller = MarshalFactory.getUnMarshaller(datatype);
      runOneTest();
   } 
   
   private void runWrappedSerializableTest(String algo) 
   throws IOException, ClassNotFoundException
   { 
      String datatype = "encryptedSerializable";
      String sd = SerializableMarshaller.DATATYPE;
      String sud = SerializableUnMarshaller.DATATYPE;
      EncryptingMarshaller em = 
         new EncryptingMarshaller(MarshalFactory.getMarshaller(sd));
      EncryptingUnMarshaller um = 
         new EncryptingUnMarshaller(MarshalFactory.getUnMarshaller(sud));
      if(algo != null)
      {
         em.setCipherAlgorithm(algo);
         um.setCipherAlgorithm(algo);
      }
      MarshalFactory.addMarshaller(datatype,em, um); 
      
      marshaller = MarshalFactory.getMarshaller(datatype);
      unmarshaller = MarshalFactory.getUnMarshaller(datatype);
   } 
}
