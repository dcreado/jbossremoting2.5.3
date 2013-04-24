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

package org.jboss.test.remoting.marshall.compress;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.compress.CompressingMarshaller;
import org.jboss.remoting.marshal.compress.CompressingUnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CompressingMarshallerTestClient extends TestCase
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


   public void testSerializable() throws IOException, ClassNotFoundException
   {
      MarshalFactory.addMarshaller(CompressingMarshaller.DATATYPE,
                                   new CompressingMarshaller(),
                                   new CompressingUnMarshaller());

      marshaller = MarshalFactory.getMarshaller(CompressingMarshaller.DATATYPE);
      unmarshaller = MarshalFactory.getUnMarshaller(CompressingMarshaller.DATATYPE);
      runOneTest();
   }


   public void testWrappedSerializable() throws IOException, ClassNotFoundException
   {
      String datatype = "compressedSerializable";
      MarshalFactory.addMarshaller(datatype,
                                   new CompressingMarshaller(MarshalFactory.getMarshaller(SerializableMarshaller.DATATYPE)),
                                   new CompressingUnMarshaller(MarshalFactory.getUnMarshaller(SerializableUnMarshaller.DATATYPE)));

      marshaller = MarshalFactory.getMarshaller(datatype);
      unmarshaller = MarshalFactory.getUnMarshaller(datatype);
      runOneTest();
   }


   public void testHTTP() throws IOException, ClassNotFoundException
   {
      String datatype = "compressedHTTP";
      MarshalFactory.addMarshaller(datatype,
                                   new CompressingMarshaller(MarshalFactory.getMarshaller(HTTPMarshaller.DATATYPE)),
                                   new CompressingUnMarshaller(MarshalFactory.getUnMarshaller(HTTPUnMarshaller.DATATYPE)));

      marshaller = MarshalFactory.getMarshaller(datatype);
      unmarshaller = MarshalFactory.getUnMarshaller(datatype);
      runOneTest();
   }


   public void testInvocations()
   {
      try
      {
         Thread.sleep(5000); // Wait for server to start.
         MarshalFactory.addMarshaller("compress", new CompressingMarshaller(), new CompressingUnMarshaller());
         String locatorURI = "socket://localhost:5400/?datatype=compress";
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
