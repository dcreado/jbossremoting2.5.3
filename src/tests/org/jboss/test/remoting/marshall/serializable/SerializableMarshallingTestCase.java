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

package org.jboss.test.remoting.marshall.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class SerializableMarshallingTestCase extends TestCase
{
   private Marshaller marshaller;
   private UnMarshaller unmarshaller;

   protected void setUp() throws Exception
   {
      super.setUp();
      marshaller = MarshalFactory.getMarshaller(SerializableMarshaller.DATATYPE);
      unmarshaller = MarshalFactory.getUnMarshaller(SerializableUnMarshaller.DATATYPE);
   }

   public void testMarshalling() throws IOException, ClassNotFoundException
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

   protected void tearDown() throws Exception
   {
      super.tearDown();
      marshaller = null;
      unmarshaller = null;
   }

}
