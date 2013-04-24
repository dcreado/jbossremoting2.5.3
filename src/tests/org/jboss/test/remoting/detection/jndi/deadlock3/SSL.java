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
package org.jboss.test.remoting.detection.jndi.deadlock3;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSL
{


   public static SSLServerSocketFactory createServerSocketFactory(String keyStorePassword, String trustStorePassword, String keyStorePath, String trustStorePath) throws Exception
   {
      // create an SSLContext
      SSLContext context = null;

      context = SSLContext.getInstance("TLS");

      // define password
      char[] keyPassphrase = keyStorePassword.toCharArray();
      char[] trustPassphrase = trustStorePassword.toCharArray();
      // load the server key store
      KeyStore server_keystore = KeyStore.getInstance("JKS");
      server_keystore.load(new FileInputStream(keyStorePath), keyPassphrase);

      // load the server trust store
      KeyStore server_truststore = KeyStore.getInstance("JKS");
      server_truststore.load(new FileInputStream(trustStorePath), trustPassphrase);

      // initialize a KeyManagerFactory with the KeyStore
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(server_keystore, keyPassphrase);
      // KeyManagers from the KeyManagerFactory
      KeyManager[] keyManagers = kmf.getKeyManagers();

      // initialize a TrustManagerFactory with the TrustStore
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(server_truststore);
      // TrustManagers from the TrustManagerFactory
      TrustManager[] trustManagers = tmf.getTrustManagers();

      // initialize context with Keystore and Truststore information
      context.init(keyManagers, trustManagers, null);

      // get ServerSocketFactory from context
      SSLServerSocketFactory ssf = context.getServerSocketFactory();
      return new ClientAuthSocketFactory(ssf);
   }

   public static SSLSocketFactory createSocketFactory(String keyStorePassword, String trustStorePassword, String keyStorePath, String trustStorePath) throws Exception
   {
      // create an SSLContext
      SSLContext context = null;

      context = SSLContext.getInstance("TLS");

      // define password
      char[] keyPassphrase = keyStorePassword.toCharArray();
      char[] trustPassphrase = trustStorePassword.toCharArray();
      // load the server key store
      KeyStore server_keystore = KeyStore.getInstance("JKS");
      server_keystore.load(new FileInputStream(keyStorePath), keyPassphrase);

      // load the server trust store
      KeyStore server_truststore = KeyStore.getInstance("JKS");
      server_truststore.load(new FileInputStream(trustStorePath), trustPassphrase);

      // initialize a KeyManagerFactory with the KeyStore
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(server_keystore, keyPassphrase);
      // KeyManagers from the KeyManagerFactory
      KeyManager[] keyManagers = kmf.getKeyManagers();

      // initialize a TrustManagerFactory with the TrustStore
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(server_truststore);
      // TrustManagers from the TrustManagerFactory
      TrustManager[] trustManagers = tmf.getTrustManagers();

      // initialize context with Keystore and Truststore information
      context.init(keyManagers, trustManagers, null);

      // get ServerSocketFactory from context
      return context.getSocketFactory();
   }

}