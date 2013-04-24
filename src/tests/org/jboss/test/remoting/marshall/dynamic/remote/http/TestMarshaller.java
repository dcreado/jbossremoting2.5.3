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

package org.jboss.test.remoting.marshall.dynamic.remote.http;

import java.io.IOException;
import java.io.OutputStream;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.test.remoting.marshall.dynamic.remote.TestWrapper;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestMarshaller extends HTTPMarshaller
{
   public final static String DATATYPE = "test";

   //private TestWrapper wrapper = null;

   /**
    * Take the data object and write to the output.  Has ben customized
    * for working with ObjectOutputStreams since requires extra messaging.
    *
    * @param dataObject Object to be writen to output
    * @param output     The data output to write the object
    *                   data to.
    */
   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      TestWrapper wrapper = new TestWrapper(dataObject);
      super.write(wrapper, output, version);
   }

   public String getDataType()
   {
      return DATATYPE;
   }
}