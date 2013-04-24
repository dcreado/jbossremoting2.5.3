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
package org.jboss.test.remoting.transport.servlet.marshal.config;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.test.remoting.marshall.config.ConfigurationMapTestParent;

/**
 * Unit tests for JBREM-1102.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Mar 21, 2009
 * </p>
 */
public class ServletConfigurationMapTestClient extends ConfigurationMapTestParent
{
   protected static Logger log = Logger.getLogger(ServletConfigurationMapTestClient.class);
   
   public void testDatatypeConfigDefault() throws Throwable
   {
      log.info("skipping " + getName());
   }
   
   public void testDatatypePassConfigMapFalse() throws Throwable
   {
      log.info("skipping " + getName());
   }
   
   public void testFQNConfigDefault() throws Throwable
   {
      log.info("skipping " + getName());
   }
   
   public void testFQNPassConfigMapFalse() throws Throwable
   {
      log.info("skipping " + getName());
   }
   
   protected int marshallerCountDatatype()
   {
      return 2;
   }
   
   protected int unmarshallerCountDatatype()
   {
      return 2;
   }
   
   protected int marshallerCountFQN()
   {
      return 1;
   }
   
   protected int unmarshallerCountFQN()
   {
      return 1;
   }
   
   protected String getTransport()
   {
      return "servlet";
   }
   
   protected void setupServer(String parameter, Map extraConfig) throws Exception
   {
      locatorURI = "servlet://localhost:8080/servlet-invoker/ServerInvokerServlet/?" + parameter;
//                   "marshaller=org.jboss.test.remoting.marshall.config.ConfigTestMarshaller&" +
//                   "unmarshaller=org.jboss.test.remoting.marshall.config.ConfigTestUnmarshaller";
         
      log.info("setting InvokerLocator to " + locatorURI);
   }
}

