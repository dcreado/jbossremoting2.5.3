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
package org.jboss.test.remoting.transport.http.chunked;

import org.jboss.remoting.marshal.UnMarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ChunkedUnMarshaller implements UnMarshaller
{
   public Object read(InputStream inputStream, Map metadata) throws IOException, ClassNotFoundException
   {
      int read = 0;
      do
      {
         read = inputStream.read();
      }
      while (read != -1);

      return "barfoo";
   }

   public void setClassLoader(ClassLoader classloader)
   {
      //TODO: -TME Implement
   }

   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      return new ChunkedUnMarshaller();
   }
}