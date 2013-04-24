/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.transport.servlet.marshal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;


/**
 * Part of unit tests for JBREM-1145.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Aug 20, 2009
 * </p>
 */
public class TestUnMarshaller extends HTTPUnMarshaller
{
   public static ArrayList unmarshallers = new ArrayList();
   private static final long serialVersionUID = -6422222480047910351L;
   public String type;
   
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {
      Object o = metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE);
      if (o instanceof List)
      {
         type = (String) ((List) o).get(0);
      }
      else if (o instanceof String)
      {
         type = (String) o;
      }
      else 
      {
         o = metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_LC);
         log.info("remotingcontenttype: " + o);
         if (o instanceof List)
         {
            type = (String) ((List) o).get(0);
         }
         else if (o instanceof String)
         {
            type = (String) o;
         }
         else 
         {
            log.warn(this + " unrecognized remotingContentType: " + o);
         } 
      }
      
      o = super.read(inputStream, metadata, version);
      log.info(this + " read " + o);
      return o;
   }
   
   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      TestUnMarshaller unmarshaller = new TestUnMarshaller();
      unmarshallers.add(unmarshaller);
      unmarshaller.setClassLoader(this.customClassLoader);
      log.info("returning " + unmarshaller, new Exception());
      return unmarshaller;
   }
}
