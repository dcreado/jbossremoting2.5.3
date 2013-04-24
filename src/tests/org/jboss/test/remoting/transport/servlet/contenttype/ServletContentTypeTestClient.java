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
package org.jboss.test.remoting.transport.servlet.contenttype;

import org.apache.log4j.Logger;
import org.jboss.test.remoting.transport.http.contenttype.ContentTypeTestCase;

/**
 * Unit tests for JBREM-1101.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Sep 1, 2009
 * </p>
 */
public class ServletContentTypeTestClient extends ContentTypeTestCase
{
   protected static Logger log = Logger.getLogger(ServletContentTypeTestClient.class);
   
   protected void setupServer(String contentType) throws Exception
   {
      locatorURI = "http://localhost:8080/servlet-invoker/ServerInvokerServlet";
      
      if (CONTENT_TYPE.equals(contentType))
      {
         locatorURI += "/OK";
      }
      if (INVALID_CONTENT_TYPE_CR.equals(contentType))
      {
         locatorURI += "/CR";
      }
      else if (INVALID_CONTENT_TYPE_LF.equals(contentType))
      {
         locatorURI += "/LF";
      }
      
      log.info("setting InvokerLocator to " + locatorURI);
   }
}

