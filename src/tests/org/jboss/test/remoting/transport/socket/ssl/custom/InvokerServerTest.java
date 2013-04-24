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

package org.jboss.test.remoting.transport.socket.ssl.custom;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the concrete test for invoker server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerServerTest extends ServerTestCase implements SSLInvokerConstants
{
   protected int serverPort = port;
   protected Connector connector = null;

   private static final Logger log = Logger.getLogger(InvokerServerTest.class);

   public void init(Map metatdata) throws Exception
   {
      if(serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }
      log.debug("port = " + serverPort);

      Map config = new HashMap();
//      config.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_TYPE, "JKS");
//      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
//      config.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
//      config.put(RemotingSSLSocketFactory.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
//      config.put(RemotingSSLSocketFactory.REMOTING_USE_CLIENT_MODE, "false");
      
//      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
//      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
//      trustStoreFilePath = "output/tests/classes/org/jboss/test/remoting/transport/socket/ssl/.truststore";
//      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
//      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
      
      Connector connector = new Connector(config);
      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metatdata));
      connector.setInvokerLocator(locator.getLocatorURI());

//      ServerSocketFactory svrSocketFactory = createServerSocketFactory();
//      connector.setServerSocketFactory(svrSocketFactory);

      connector.create();

      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();
   }
   
   protected String getTransport()
   {
      return transport;
   }
   
   private ServerSocketFactory createServerSocketFactory()
         throws NoSuchAlgorithmException, KeyManagementException, IOException,
                CertificateException, UnrecoverableKeyException, KeyStoreException
   {
      ServerSocketFactory serverSocketFactory = null;

      SSLSocketBuilder server = new SSLSocketBuilder();
      server.setUseSSLServerSocketFactory(false);

      server.setSecureSocketProtocol("SSL");
      server.setKeyStoreAlgorithm("SunX509");

      server.setKeyStoreType("JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      server.setKeyStoreURL(keyStoreFilePath);
      server.setKeyStorePassword("unit-tests-server");
      /*
       * This is optional since if not set, will use
       * the key store password (and are the same in this case)
       */
      //server.setKeyPassword("unit-tests-server");

      serverSocketFactory = server.createSSLServerSocketFactory();

      return serverSocketFactory;
   }

   protected String buildLocatorURI(Map metadata)
   {
      if(metadata == null || metadata.size() == 0)
      {
         return getTransport() + "://localhost:" + serverPort;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(getTransport() + "://localhost:" + serverPort);

         Set keys = metadata.keySet();
         if(keys.size() > 0)
         {
            uriBuffer.append("/?");
         }

         Iterator itr = keys.iterator();
         while(itr.hasNext())
         {
            String key = (String) itr.next();
            String value = (String) metadata.get(key);
            uriBuffer.append(key + "=" + value + "&");
         }
         return uriBuffer.substring(0, uriBuffer.length() - 1);
      }
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }

   protected void setUp() throws Exception
   {
      init(null);
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      InvokerServerTest test = new InvokerServerTest();
      try
      {
         test.setUp();

         Thread.currentThread().sleep(20000);

         test.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }


}
