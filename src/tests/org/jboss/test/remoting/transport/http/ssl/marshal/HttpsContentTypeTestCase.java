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
package org.jboss.test.remoting.transport.http.ssl.marshal;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import org.jboss.test.remoting.transport.http.marshal.HttpContentTypeTestCase;

/*
 * Unit tests for JBREM-1145
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Aug 19, 2009
 * </p>
 */
public class HttpsContentTypeTestCase extends HttpContentTypeTestCase
{
   private static Logger log = Logger.getLogger(HttpsContentTypeTestCase.class);
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
         log.info("keystore: " + keyStoreFilePath);
         System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
         System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         log.info("truststore: " + trustStoreFilePath);
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
         System.setProperty("javax.net.ssl.trustStorePassword", "unit-tests-client");
      }
      super.setUp();
   }
   
   protected String getTransport()
   {
      return "https";
   }
   
   protected void addExtraClientConfig(Map config)
   {
      config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
   }
}

