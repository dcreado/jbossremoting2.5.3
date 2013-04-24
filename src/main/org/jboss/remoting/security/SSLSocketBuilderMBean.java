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
package org.jboss.remoting.security;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.security.Provider;
import java.security.SecureRandom;

/**
 * The service interface of the socket builder.
 *
 * @author  <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @author  <a href="mailto:telrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 1348 $
 */
public interface SSLSocketBuilderMBean
{
   /**
    * Setting the flag that indicates if this class should use <code>SSLServerSocketFactory.getDefault()</code>
    * when creating the ServerSocketFactory to use (when calling {@link #createSSLServerSocketFactory()}). If
    * <code>true</code>, will allow for setting key store location (via <code>javax.net.ssl.keyStore</code> system
    * property) and setting of the key store password (via <code>javax.net.ssl.keyStorePassword</code> system
    * property) and no other configuration is needed (none of the other setters will need to be called and are in
    * fact ignored). If set to <code>false</code>, will allow the custom setting of secure socket protocol, key
    * store management algorithm, file location, password among other things.
    *
    * <p/>The default value is <code>true</code>.
    *
    * <p/><b>NOTE: If this is not explicitly set to <code>false</code>, no customizations can be made and the
    * default implementation provided by the JVM vendor being used will be executed.</b>
    *
    * @param shouldUse
    */
   void setUseSSLServerSocketFactory( boolean shouldUse );

   /**
    * Return whether <code>SSLServerSocketFactory.getDefault()</code> will be used or not. See
    * {@link #setUseSSLServerSocketFactory(boolean)} for more information on what this means.
    *
    * @return the flag to indicate if the default server socket factory is used
    */
   boolean getUseSSLServerSocketFactory();

   /**
    * Setting the flag that indicates if this class should use <code>SSLSocketFactory.getDefault()</code> when
    * creating the SocketFactory to use (when calling {@link #createSSLSocketFactory()}). If <code>true</code>,
    * will allow for setting trust store location (via <code>javax.net.ssl.trustStore</code> system property) and
    * setting of the key store password (via <code>javax.net.ssl.trustStorePassword</code> system property) and no
    * other configuration is needed (none of the other setters will need to be called and are in fact ignored). If
    * set to <code>false</code>, will allow the custom setting of secure socket protocol, key store management
    * algorithm, file location, password among other things.
    *
    * <p/>The default value is <code>true</code>.
    *
    * <p/><b>NOTE: If this is not explicitly set to <code>false</code>, no customizations can be made and the
    * default implementation provided by the JVM vendor being used will be executed.</b>
    *
    * @param shouldUse
    */
   void setUseSSLSocketFactory( boolean shouldUse );

   /**
    * Return whether <code>SSLSocketFactory.getDefault()</code> will be used or not. See
    * {@link #setUseSSLSocketFactory(boolean)} for more information on what this means.
    *
    * @return the flag to indicate if the default socket factory is used
    */
   boolean getUseSSLSocketFactory();

   /**
    * Will create a <code>SSLServerSocketFactory</code>. If the {@link #getUseSSLServerSocketFactory()} property is
    * set to <code>true</code> (which is the default), it will use <code>SSLServerSocketFactory.getDefault()</code>
    * to get the server socket factory. Otherwise, if property is <code>false</code>, will use all the other custom
    * properties that have been set to create a custom server socket factory.
    *
    * @return the server socket factory that has been created
    *
    * @throws IOException
    */
   ServerSocketFactory createSSLServerSocketFactory()
   throws IOException;

   /**
    * Will create a <code>SSLServerSocketFactory</code>. If the {@link #getUseSSLServerSocketFactory()} property is
    * set to <code>true</code> (which is the default), it will use <code>SSLServerSocketFactory.getDefault()</code>
    * to get the server socket factory. Otherwise, if property is <code>false</code>, will use all the other custom
    * properties that have been set to create a custom server socket factory. The given custom factory will be used
    * as the wrapper around the factory created by this method and will be the factory returned. If it is <code>
    * null</code>, one will be created and returned.
    *
    * @param  wrapper the wrapper that will contain the created factory - used so the caller can further customize
    *                 the factory and its sockets as desired (may be <code>null</code>)
    *
    * @return the server socket factory that has been created (may be wrapper if it was not <code>null</code>)
    *
    * @throws IOException
    */
   ServerSocketFactory createSSLServerSocketFactory( CustomSSLServerSocketFactory wrapper )
   throws IOException;

   /**
    * Will create a <code>SSLSocketFactory</code>. If the {@link #getUseSSLSocketFactory()} property is set to
    * <code>true</code> (which is the default), it will use <code>SSLSocketFactory.getDefault()</code> to get the
    * socket factory. Otherwise, if property is <code>false</code>, will use all the other custom properties that
    * have been set to create a custom server socket factory.
    *
    * @return the server socket factory that has been created
    *
    * @throws IOException
    */
   SocketFactory createSSLSocketFactory()
   throws IOException;

   /**
    * Will create a <code>SSLSocketFactory</code>. If the {@link #getUseSSLSocketFactory()} property is set to
    * <code>true</code> (which is the default), it will use <code>SSLSocketFactory.getDefault()</code> to get the
    * socket factory. Otherwise, if property is <code>false</code>, will use all the other custom properties that
    * have been set to create a custom server socket factory. The given custom factory will be used as the wrapper
    * around the factory created by this method and will be the factory returned. If it is <code>null</code>, one
    * will be created and returned.
    *
    * @param  wrapper the wrapper that will contain the created factory - used so the caller can further customize
    *                 the factory and its sockets as desired (may be <code>null</code>)
    *
    * @return the server socket factory that has been created (may be wrapper if it was not <code>null</code>)
    *
    * @throws IOException
    */
   SocketFactory createSSLSocketFactory( CustomSSLSocketFactory wrapper )
   throws IOException;

   /**
    * Returns the SSL context that will create the server socket factories. This returns <code>null</code> until
    * the context is initialized.
    *
    * @return the SSL context or <code>null</code> if it hasn't been initialized yet
    */
   SSLContext getServerSocketFactorySSLContext();

   /**
    * Returns the SSL context that will create the socket factories. This returns <code>null</code> until the
    * context is initialized.
    *
    * @return the SSL context or <code>null</code> if it hasn't been initialized yet
    */
   SSLContext getSocketFactorySSLContext();

   /**
    * Returns the name of the secure socket protocol to be used by the sockets created by our factories.
    *
    * @return the secure socket protocol name (e.g. TLS)
    */
   String getSecureSocketProtocol();

   /**
    * Sets the name of the secure socket protocol to be used by the sockets created by our factories.
    *
    * @param protocol the secure socket protocol name (e.g. TLS)
    */
   void setSecureSocketProtocol( String protocol );

   /**
    * Returns the Cryptographic Service Provider which supplies a concrete implementation of a subset of the Java 2
    * SDK Security API cryptography features.
    *
    * @return the provider (will be <code>null</code> if not specifically {@link #setProvider(Provider) set})
    */
   Provider getProvider();

   /**
    * Sets the Cryptographic Service Provider which supplies a concrete implementation of a subset of the Java 2
    * SDK Security API cryptography features.
    *
    * @param provider the provider this object's SSL context should use
    */
   void setProvider( Provider provider );

   /**
    * Returns the name of the Cryptographic Service Provider which refers to a package or set of packages that
    * supply a concrete implementation of a subset of the Java 2 SDK Security API cryptography features.
    *
    * @return identifies by name the provider this object's SSL context should use (will be <code>null</code>
    *         if not specifically {@link #setProviderName(String) set} or found in the configuration)
    */
   String getProviderName();

   /**
    * Sets the name of the Cryptographic Service Provider which refers to a package or set of packages that supply
    * a concrete implementation of a subset of the Java 2 SDK Security API cryptography features.
    *
    * @param providerName identifies by name the provider this object's SSL context should use
    */
   void setProviderName( String providerName );

   /**
    * Returns the secure random used by this object's SSL context. If this object does not yet have a secure random
    * object, one is created here.
    *
    * @return the secure random object
    */
   SecureRandom getSecureRandom();

   /**
    * Provides this class with the SecureRandom object to use when initializing the SSL contexts.
    *
    * @param secureRandom
    */
   void setSecureRandom( SecureRandom secureRandom );

   /**
    * Returns the path to the key store as a URL.
    *
    * @return path to keystore
    */
   URL getKeyStore();

   /**
    * Returns the path to the key store as a String.
    * @return path to keystore
    */
   String getKeyStoreURL();

   /**
    * Sets the path to the keystore file. This can be relative to the classloader or can be an absolute path to
    * someplace on the file system or can be a URL string. If the path is not valid, a runtime exception is thrown.
    *
    * @param keyStoreFilePath
    */
   void setKeyStoreURL( String keyStoreFilePath );

   /**
    * Sets the path to the keystore file as a URL
    * @param keyStoreURL
    */
   void setKeyStore(URL keyStoreURL);

   /**
    * Returns the keystore's file type. This is typically "JKS".
    *
    * @return keystore file type.
    */
   String getKeyStoreType();

   /**
    * Sets the keystore's file type. Typically this is "JKS".
    *
    * @param keyStoreType
    */
   void setKeyStoreType( String keyStoreType );

   /**
    * Returns the algorithm used to manage the keys in the keystore.
    *
    * @return the key management algorithm
    */
   String getKeyStoreAlgorithm();

   /**
    * Sets the algorithm used to manage the keys in the keystore.
    *
    * @param algorithm
    */
   void setKeyStoreAlgorithm( String algorithm );

   /**
    * Sets the password used to gain access to the keystore.
    *
    * @param keyStorePassword
    */
   void setKeyStorePassword( String keyStorePassword );

   /**
    * Gets the path to the truststore file.
    *
    * @return path to truststore
    */
   URL getTrustStore();

   /**
    * Gets the path to the truststore file.
    *
    * @return path to truststore
    */
   String getTrustStoreURL();

   /**
    * Sets the path to the truststore file. This can be relative to the classloader or can be an absolute path to
    * someplace on the file system or can be a URL string. If the path is not valid, a runtime exception is thrown.
    *
    * @param trustStoreFilePath path to truststore
    */
   void setTrustStoreURL( String trustStoreFilePath );

   /**
    * Sets the path to the truststore file. This can be relative to the classloader or can be an absolute path to
    * someplace on the file system or can be a URL string. If the path is not valid, a runtime exception is thrown.
    *
    * @param trustStore path to truststore
    */
   void setTrustStore( URL trustStore );

   /**
    * Gets the truststore's file type. Typically this is "JKS". If not set, the key store file type is used or the
    * default if that isn't set.
    *
    * @return the truststore file type
    */
   String getTrustStoreType();

   /**
    * Sets the truststore's file type. Typically this is "JKS".
    *
    * @param trustStoreType
    */
   void setTrustStoreType( String trustStoreType );

   /**
    * Returns the algorithm used to manage the keys in the truststore.
    *
    * @return the key management algorithm
    */
   String getTrustStoreAlgorithm();

   /**
    * Sets the algorithm used to manage the keys in the truststore.
    *
    * @param algorithm
    */
   void setTrustStoreAlgorithm( String algorithm );

   /**
    * Sets the password used to gain access to the truststore.
    *
    * @param trustStorePassword
    */
   void setTrustStorePassword( String trustStorePassword );

   /**
    * Returns the key alias used to identify the client's key in the keystore.
    *
    * @return the client key alias
    */
   String getKeyAlias();

   /**
    * Sets the key alias used to identify the client's key in the keystore.
    *
    * @param alias the client key alias
    */
   void setKeyAlias( String alias );

   /**
    * Sets the password to use for the keys within the key store.
    *
    * @param keyPassword
    */
   void setKeyPassword( String keyPassword );

   /**
    * Returns the flag to indicate if the sockets created by the factories should be in client mode.
    *
    * @return <code>true</code> if sockets should be in client mode
    */
   boolean isSocketUseClientMode();

   /**
    * Returns the flag to indicate if the server sockets created by the factories should be in client mode.
    *
    * @return <code>true</code> if sockets should be in client mode
    */
   boolean isServerSocketUseClientMode();

   /**
    * Sets the flag to indicate if the sockets created by the factories should be in client mode.
    *
    * @param useClientMode <code>true</code> if sockets should be in client mode
    */
   void setSocketUseClientMode( boolean useClientMode );

   /**
    * Sets the flag to indicate if the server sockets created by the factories should be in client mode.
    *
    * @param useClientMode <code>true</code> if sockets should be in client mode
    */
   void setServerSocketUseClientMode( boolean useClientMode );

   /**
    * Determines if there should be no client authentication. This is only used for sockets in
    * server mode (see <code>SSLSocket.getUseClientMode</code>).
    *
    * @return <code>true</code> if client authentication should be disabled.
    */
   boolean isClientAuthModeNone();

   /**
    * Determines if there should be client authentication but it isn't required. This is only used for sockets in
    * server mode (see <code>SSLSocket.getUseClientMode</code>).
    *
    * @return <code>true</code> if client authentication should be enabled but isn't required.
    */
   boolean isClientAuthModeWant();

   /**
    * Determines if there must be client authentication - it is required. This is only used for sockets in
    * server mode (see <code>SSLSocket.getUseClientMode</code>).
    *
    * @return <code>true</code> if client authentication is required
    */
   boolean isClientAuthModeNeed();

   /**
    * Returns the client authentication mode to say if sockets will not require client authentication, will want
    * client auth but not require it or to require it. This is only used for sockets in
    * server mode (see <code>SSLSocket.getUseClientMode</code>).
    *
    * <p>If not set, {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE} is returned.</p>
    *
    * @return client auth mode
    *
    * @see    SSLSocketBuilder#CLIENT_AUTH_MODE_NONE
    * @see    SSLSocketBuilder#CLIENT_AUTH_MODE_WANT
    * @see    SSLSocketBuilder#CLIENT_AUTH_MODE_NEED
    */
   String getClientAuthMode();

   /**
    * Sets the client authentication mode to say if sockets will not require client authentication, will want
    * client auth but not require it or to require it.  This is only used for sockets in
    * server mode (see <code>SSLSocket.getUseClientMode</code>).
    *
    * <p>If <code>mode</code> is invalid or <code>null</code>, will default to
    * {@link SSLSocketBuilder#CLIENT_AUTH_MODE_NONE}.</p>
    *
    * @param mode client auth mode
    *
    * @see   SSLSocketBuilder#CLIENT_AUTH_MODE_NONE
    * @see   SSLSocketBuilder#CLIENT_AUTH_MODE_WANT
    * @see   SSLSocketBuilder#CLIENT_AUTH_MODE_NEED
    */
   void setClientAuthMode( String mode );

   /**
    * Returns the server authentication mode to say if a client socket will require to authenticate a server certificate
    * as trustworthy.
    *
    * @return server auth mode
    */
   boolean isServerAuthMode();

   /**
    * Sets the server authentication mode to say if a client socket will require to authenticate a server certificate
    * as trustworthy.
    *
    * @param mode server auth mode
    */
   void setServerAuthMode( boolean mode );

   /**
    * Creates a clone.
    *
    * @return
    */
   Object clone();

   /**
    * No-op - just needed for MBean service API.
    *
    * @throws Exception
    */
   void create()
   throws Exception;

   /**
    * No-op - just needed for MBean service API. Create already called at this point.
    *
    * @throws Exception
    */
   void start()
   throws Exception;

   /**
    * No-op - just needed for MBean server API.
    */
   void stop();

   /**
    * No-op - just needed for MBean server API.
    */
   void destroy();
}