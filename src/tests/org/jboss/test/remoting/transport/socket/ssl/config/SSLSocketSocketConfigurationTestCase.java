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
package org.jboss.test.remoting.transport.socket.ssl.config;

import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.sslsocket.SSLSocketServerInvoker;
import org.jboss.test.remoting.transport.socket.configuration.SocketServerSocketConfigurationTestCase;
import org.jboss.test.remoting.transport.socket.configuration.SocketSocketConfigurationTestCase;


/**
 * Unit test for JBREM-703.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 28, 2008
 * </p>
 */
public class SSLSocketSocketConfigurationTestCase
extends SocketSocketConfigurationTestCase
{
   private static Logger log = Logger.getLogger(SSLSocketSocketConfigurationTestCase.class);
   
   static protected String[] cipherSuites;
   static protected String[] protocols;

   
   public void setUp() throws Exception
   {  
      if (firstTime)
      {
         super.setUp();
         
         SSLSocket s = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
         
         String[] strings = s.getSupportedCipherSuites();
         log.info("supported cipher suites: ");
         for (int i = 0; i < strings.length; i++)
            log.info("  " + strings[i]);
         strings = s.getEnabledCipherSuites();
         log.info("enabled cipher suites: ");
         for (int i = 0; i < strings.length; i++)
            log.info("  " + strings[i]);
         strings = s.getSupportedProtocols();
         log.info("supported protocols: ");
         for (int i = 0; i < strings.length; i++)
            log.info("  " + strings[i]);
         strings = s.getEnabledProtocols();
         log.info("enabled protocols: ");
         for (int i = 0; i < strings.length; i++)
            log.info("  " + strings[i]);

         strings = s.getSupportedCipherSuites();
         int len = strings.length - 1;
         cipherSuites = new String[len];
         for (int i = 0; i < len; i++)
            cipherSuites[i] = strings[i];
         
         strings = s.getSupportedProtocols();
         len = strings.length - 1;
         protocols = new String[len];
         for (int i = 0; i < len; i++)
            protocols[i] = strings[i];
         
         log.info("using cipherSuites: ");
         for (int i = 0; i < cipherSuites.length; i++)
            log.info("  " + cipherSuites[i]);
         log.info("using protocols: ");
         for (int i = 0; i < protocols.length; i++)
            log.info("  " + protocols[i]);
      }
   }

   
   public void tearDown()
   {
   }
   
   
   protected void setupClient(Map config)
   {
      super.setupClient(config);
      config.put("enabledCipherSuites", cipherSuites);
      config.put("enabledProtocols", protocols);
      config.put("enableSessionCreation", "true");
   }
   
   
   protected void doSocketTest(Socket s) throws SocketException
   {
      assertTrue(s.getKeepAlive());
      suggestEquals(2345, s.getReceiveBufferSize(), "receiveBufferSize");
      suggestEquals(3456, s.getSendBufferSize(), "sendBufferSize");
      assertEquals(4567, s.getSoLinger());
      suggestEquals(0, s.getTrafficClass(), "trafficClass");

      assertTrue(s instanceof SSLSocket);
      SSLSocket ss = (SSLSocket) s;
      assertTrue(ss.getEnableSessionCreation());
      
      log.info("actual enabledCipherSuites: ");
      String[] strings = ss.getEnabledCipherSuites();
      for (int i = 0; i < strings.length; i++)
         log.info("  " + strings[i]);
      assertEquals(cipherSuites, ss.getEnabledCipherSuites());
      assertEquals(protocols, ss.getEnabledProtocols());
   }
   
   
   protected void assertEquals(String[] strings1, String[] strings2)
   {
      HashSet set1 = new HashSet();
      for (int i = 0; i < strings1.length; i++)
         set1.add(strings1[i]);
      HashSet set2 = new HashSet();
      for (int i = 0; i < strings2.length; i++)
         set2.add(strings2[i]);
      assertEquals(set1, set2);
   }
   
   
   protected String getTransport()
   {
      return "sslsocket";
   }
   
   
   protected void addExtraClientConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
   }
}