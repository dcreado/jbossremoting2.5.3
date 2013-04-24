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

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator; 
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller; 
import org.jboss.remoting.marshal.encryption.EncryptingMarshaller;
import org.jboss.remoting.marshal.encryption.EncryptingUnMarshaller;  
import org.jboss.remoting.serialization.SerializationStreamFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException; 

/**
 * Test Client for the marshaller that can do encryption
 * @author Anil.Saldhana@jboss.org
 */
public class EncryptingMarshallerTestClient extends TestCase
{
   private Marshaller marshaller;
   private UnMarshaller unmarshaller;

   protected void setUp() throws Exception
   {
      super.setUp();
   }


   protected void tearDown() throws Exception
   {
      super.tearDown();
      marshaller = null;
      unmarshaller = null;
   } 
   
   public void testJavaInvocations()
   {
      try
      {
         Thread.sleep(5000); // Wait for server to start.
         MarshalFactory.addMarshaller("encrypt", 
               new EncryptingMarshaller(), 
               new EncryptingUnMarshaller());
         String locatorURI = "socket://localhost:5300/?datatype=encrypt&"+
            InvokerLocator.SERIALIZATIONTYPE + "=" +
            SerializationStreamFactory.JAVA_ENCRYPT;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         Client remotingClient = new Client(locator);
         remotingClient.connect();
         String request1 = "First request";
         Object response1 = remotingClient.invoke(request1);
         assertTrue(request1.equals(response1));
         Object request2 = new Integer(17);
         Object response2 = remotingClient.invoke(request2);
         assertTrue(request2.equals(response2));
      }
      catch(Throwable e)
      {
         e.printStackTrace();
         fail();
      }
   } 
   
   public void testJBossInvocations()
   {
      try
      {
         Thread.sleep(5000); // Wait for server to start.
         MarshalFactory.addMarshaller("encrypt", 
               new EncryptingMarshaller(), 
               new EncryptingUnMarshaller());
         String locatorURI = "socket://localhost:5400/?datatype=encrypt&"+
            InvokerLocator.SERIALIZATIONTYPE + "=" +
            SerializationStreamFactory.JBOSS_ENCRYPT;
         InvokerLocator locator = new InvokerLocator(locatorURI); 
         Client remotingClient = new Client(locator);
         remotingClient.connect();
         String request1 = "First request";
         Object response1 = remotingClient.invoke(request1);
         assertTrue(request1.equals(response1));
         Object request2 = new Integer(17);
         Object response2 = remotingClient.invoke(request2);
         assertTrue(request2.equals(response2));
      }
      catch(Throwable e)
      {
         e.printStackTrace();
         fail();
      }
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
}
