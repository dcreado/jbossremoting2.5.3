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
package org.jboss.remoting.transport.coyote;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResponseMap implements Map
{

   Map internalMap = new HashMap();
   /**
    * The associated output buffer.
    */
   protected OutputBuffer outputBuffer = new OutputBuffer();

   /**
    * ServletOutputStream.
    */
   protected CoyoteOutputStream outputStream =
         new CoyoteOutputStream(outputBuffer);

   /**
    * Coyote response.
    */
   protected org.apache.coyote.Response coyoteResponse;

   /**
    * Set the Coyote response.
    *
    * @param coyoteResponse The Coyote response
    */
   public void setCoyoteResponse(org.apache.coyote.Response coyoteResponse)
   {
      this.coyoteResponse = coyoteResponse;
      outputBuffer.setResponse(coyoteResponse);
   }

   /**
    * Get the Coyote response.
    */
   public org.apache.coyote.Response getCoyoteResponse()
   {
      return (this.coyoteResponse);
   }

   /**
    * Get the output stream.
    */
   public OutputStream getOutputStream()
   {
      return (this.outputStream);
   }

   public void recycle()
   {
      outputBuffer.recycle();
   }


   public int size()
   {
      return internalMap.size();
   }

   public boolean isEmpty()
   {
      return internalMap.isEmpty();
   }

   public boolean containsKey(Object key)
   {
      return internalMap.containsKey(key);
   }

   public boolean containsValue(Object value)
   {
      return internalMap.containsValue(value);
   }

   public Object get(Object key)
   {
      return internalMap.get(key);
   }

   public Object put(Object arg0, Object arg1)
   {
      Object obj = internalMap.put(arg0, arg1);
      coyoteResponse.setHeader(String.valueOf(arg0), String.valueOf(arg1));
      return obj;
   }

   public Object remove(Object key)
   {
      return internalMap.remove(key);
   }

   public void putAll(Map arg0)
   {
      internalMap.putAll(arg0);
   }

   public void clear()
   {
      internalMap.clear();

   }

   public Set keySet()
   {
      return internalMap.keySet();
   }

   public Collection values()
   {
      return internalMap.values();
   }

   public Set entrySet()
   {
      return internalMap.entrySet();
   }

   public Map getMap()
   {
      return internalMap;
   }
}
