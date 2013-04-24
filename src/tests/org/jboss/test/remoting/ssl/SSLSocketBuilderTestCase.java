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
package org.jboss.test.remoting.ssl;

import junit.framework.TestCase;
import org.jboss.remoting.security.SSLSocketBuilder;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case to validate behavior of SSLSocketBuilder.
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketBuilderTestCase extends TestCase
{
   /**
    * Test is specifically to verify can have different keystore password
    * and key password (per JBREM-488)
    * @throws IOException
    */
   public void testKeyPassword() throws IOException
   {
      SSLSocketBuilder socketBuilder = new SSLSocketBuilder();
      socketBuilder.setUseSSLServerSocketFactory(false);

      socketBuilder.setSecureSocketProtocol("TLS");
      socketBuilder.setKeyStoreAlgorithm("SunX509");

      socketBuilder.setKeyStoreType("JKS");
      String keyStoreFilePath = this.getClass().getResource("ssl.keystore").getFile();
      socketBuilder.setKeyStoreURL(keyStoreFilePath);
      socketBuilder.setKeyStorePassword("foobar");
      socketBuilder.setKeyPassword("barfoo");

      ServerSocketFactory svrSocketFactory = socketBuilder.createSSLServerSocketFactory();
      assertNotNull(svrSocketFactory);
   }
   
   /**
    * Added for JBREM-510.
    */
   public void testSocketFactoryInServerMode() throws Throwable
   {
      try
      {
         Map config = new HashMap();
         config.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
         SSLSocketBuilder socketBuilder = new SSLSocketBuilder(config);
         socketBuilder.setUseSSLSocketFactory(false);
         socketBuilder.createSSLSocketFactory();
         fail("should have thrown IOException");
      }
      catch (IOException e)
      {
         assertEquals("Error initializing socket factory SSL context: Can not find keystore url.", e.getMessage());
      }
   }
   
   /**
    * Added for JBREM-510.
    */
   public void testSocketFactoryInClientMode() throws IOException
   {
      SSLSocketBuilder socketBuilder = new SSLSocketBuilder();
      socketBuilder.setUseSSLServerSocketFactory(false);

      socketBuilder.setSecureSocketProtocol("TLS");
      socketBuilder.setKeyStoreAlgorithm("SunX509");

      socketBuilder.setKeyStoreType("JKS");
      String trustStoreFilePath = this.getClass().getResource("ssl.truststore").getFile();
      socketBuilder.setTrustStoreURL(trustStoreFilePath);
      socketBuilder.setTrustStorePassword("foobar");

      SocketFactory socketFactory = socketBuilder.createSSLSocketFactory();
      assertNotNull(socketFactory);
   }

}