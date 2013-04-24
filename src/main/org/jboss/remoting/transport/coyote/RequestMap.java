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

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.tomcat.util.buf.B2CConverter;

public class RequestMap implements Map
{

   Map internalMap = new HashMap();

   /**
    * The associated input buffer.
    */
   protected InputBuffer inputBuffer = new InputBuffer();

   /**
    * ServletInputStream.
    */
   protected CoyoteInputStream inputStream =
         new CoyoteInputStream(inputBuffer);

   /**
    * Coyote request.
    */
   protected org.apache.coyote.Request coyoteRequest;

   /**
    * Set the Coyote request.
    *
    * @param coyoteRequest The Coyote request
    */
   public void setCoyoteRequest(org.apache.coyote.Request coyoteRequest)
   {
      this.coyoteRequest = coyoteRequest;
      inputBuffer.setRequest(coyoteRequest);
   }

   /**
    * Get the Coyote request.
    */
   public org.apache.coyote.Request getCoyoteRequest()
   {
      return (this.coyoteRequest);
   }

   /**
    * Get the input stream.
    */
   public InputStream getInputStream()
   {
      return (this.inputStream);
   }

   /**
    * URI byte to char converter (not recycled).
    */
   protected B2CConverter URIConverter = null;

   /**
    * Return the URI converter.
    */
   protected B2CConverter getURIConverter()
   {
      return URIConverter;
   }

   /**
    * Set the URI converter.
    *
    * @param URIConverter the new URI connverter
    */
   protected void setURIConverter(B2CConverter URIConverter)
   {
      this.URIConverter = URIConverter;
   }

   public void recycle()
   {
      inputBuffer.recycle();
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
      if("MethodType".equals(key))
      {
         return coyoteRequest.method().toString();
      }
      else if("Path".equals(key))
      {
         return coyoteRequest.decodedURI().toString();
      }
      else if("HttpVersion".equals(key))
      {
         return coyoteRequest.protocol().toString();
      }
      else
      {
         Object value = internalMap.get(key);
         if(value != null)
         {
            return value;
         }
         else
         {
            return coyoteRequest.getHeader(String.valueOf(key));
         }
      }
   }

   public Object put(Object arg0, Object arg1)
   {
      return internalMap.put(arg0, arg1);
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

}
