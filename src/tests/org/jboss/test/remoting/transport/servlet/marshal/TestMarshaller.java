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
import java.io.OutputStream;
import java.util.ArrayList;

import org.jboss.logging.Logger;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;


/**
 * Part of unit tests for JBREM-1145
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Aug 20, 2009
 * </p>
 */
public class TestMarshaller extends HTTPMarshaller
{
   private static Logger log = Logger.getLogger(TestMarshaller.class);

   public static ArrayList marshallers = new ArrayList();
   private static final long serialVersionUID = -7528137229006015488L;
   public String type;

   public void write(Object dataObject, OutputStream output, int version) throws IOException
   {
      log.info(this + " writing " + dataObject);
      type = (dataObject instanceof String) ? HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING : HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING;
      super.write(dataObject, output, version);
   }

   public Marshaller cloneMarshaller() throws CloneNotSupportedException
   {
      TestMarshaller marshaller = new TestMarshaller();
      marshallers.add(marshaller);
      log.info("returning " + marshaller);
      return marshaller;
   }
}
