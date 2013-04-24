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

import org.jboss.logging.Logger;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.util.socket.RemotingKeyManager;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * A class that contains code that remoting factories need to build customized server and client SSL sockets.
 *
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @author <a href="mailto:telrod@jboss.com">Tom Elrod</a>
 *
 * @version $Revision: 5689 $
 */
public class SSLSocketBuilder implements SSLSocketBuilderMBean, Cloneable
{
   /**
    * Constant defining the config property used to define the SSL provider to use.
    */
   public static final String REMOTING_SSL_PROVIDER_NAME = "org.jboss.remoting.sslProviderName";

   /**
    * Constant defining the config property used to define the SSL socket protocol to use.
    */
   public static final String REMOTING_SSL_PROTOCOL = "org.jboss.remoting.sslProtocol";

   /**
    * If the protocol isn't specified, this will be the default.
    * Value is "TLS".
    */
   public static final String DEFAULT_SSL_PROTOCOL = "TLS";

   /**
    * Constant defining the config property used to define if the sockets will be in
    * client or server mode.
    */
   public static final String REMOTING_SOCKET_USE_CLIENT_MODE = "org.jboss.remoting.socket.useClientMode";

   /**
    * Constant defining the config property used to define if the server sockets will be in
    * client or server mode.
    */
   public static final String REMOTING_SERVER_SOCKET_USE_CLIENT_MODE = "org.jboss.remoting.serversocket.useClientMode";

   /**
    * Constant defining the config property used to define if sockets need or want
    * client authentication. This configuration option is only useful for sockets in the server mode.
    * The value of such a property is one of the CLIENT_AUTH_MODE_XXX constants.
    */
   public static final String REMOTING_CLIENT_AUTH_MODE = "org.jboss.remoting.clientAuthMode";

   /**
    * Client auth mode that indicates client authentication will not be peformed.
    */
   public static final String CLIENT_AUTH_MODE_NONE = "none";

   /**
    * Client auth mode that indicates that we want client authentication but it isn't required.
    */
   public static final String CLIENT_AUTH_MODE_WANT = "want";

   /**
    * Client auth mode that indicates that client authentication is required.
    */
   public static final String CLIENT_AUTH_MODE_NEED = "need";

   /**
    * Constant defining the config property used to define if a client should attempt to
    * authenticate a server certificate as one it trusts.  The value of such a property is
    * a boolean.
    */
   public static final String REMOTING_SERVER_AUTH_MODE = "org.jboss.remoting.serverAuthMode";

   /**
    * Constant defining the config property used to define where JBoss/Remoting will
    * look for the keystore file. This can be relative to the thread's
    * classloader or can be an absolute path on the file system or can be a URL.
    */
   public static final String REMOTING_KEY_STORE_FILE_PATH = "org.jboss.remoting.keyStore";

   /**
    * Constant defining the config property that defines the keystore's type.
    */
   public static final String REMOTING_KEY_STORE_TYPE = "org.jboss.remoting.keyStoreType";

   /**
    * Constant defining the config property that defines the key management algorithm
    * used by the keystore.
    */
   public static final String REMOTING_KEY_STORE_ALGORITHM = "org.jboss.remoting.keyStoreAlgorithm";

   /**
    * Constant defining the config property that defines the password of the keystore.
    */
   public static final String REMOTING_KEY_STORE_PASSWORD = "org.jboss.remoting.keyStorePassword";

   /**
    * Constant defining the config property that indicates the client's alias as found in the keystore.
    */
   public static final String REMOTING_KEY_ALIAS = "org.jboss.remoting.keyAlias";

   /**
    * Constant defining the config property that indicates the key password for the keys in the key store.
    */
   public static final String REMOTING_KEY_PASSWORD = "org.jboss.remoting.keyPassword";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when locating the keystore file.
    */
   public static final String STANDARD_KEY_STORE_FILE_PATH = "javax.net.ssl.keyStore";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when needing to know what type the keystore file is.
    */
   public static final String STANDARD_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when needing the keystore password.
    */
   public static final String STANDARD_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";

   /**
    * Default key/trust store type if one not set as bean property, via config, or via system property.
    * Value is 'JKS'.
    */
   public static final String DEFAULT_KEY_STORE_TYPE = "JKS";

   /**
    * Default key/trust store algorithm if one net set as bean property or via config.
    * Value is 'SunX509'.
    */
   public static final String DEFAULT_KEY_STORE_ALGORITHM = "SunX509";

   /**
    * Constant defining the config property used to define where JBoss/Remoting
    * will look for the truststore file. This can be relative to the thread's
    * classloader or can be an absolute path on the file system.
    */
   public static final String REMOTING_TRUST_STORE_FILE_PATH = "org.jboss.remoting.trustStore";

   /**
    * Constant defining the config property that defines the truststore's type.
    */
   public static final String REMOTING_TRUST_STORE_TYPE = "org.jboss.remoting.trustStoreType";

   /**
    * Constant defining the config property that defines the key management
    * algorithm used by the truststore.
    */
   public static final String REMOTING_TRUST_STORE_ALGORITHM = "org.jboss.remoting.trustStoreAlgorithm";

   /**
    * Constant defining the config property that defines the password of the keystore.
    */
   public static final String REMOTING_TRUST_STORE_PASSWORD = "org.jboss.remoting.trustStorePassword";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when locating the truststore file.
    */
   public static final String STANDARD_TRUST_STORE_FILE_PATH = "javax.net.ssl.trustStore";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when needing to know what type the truststore file is.
    */
   public static final String STANDARD_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

   /**
    * Constant that defines the standard system property that the javax.net.ssl
    * classes look for when needing the truststore password.
    */
   public static final String STANDARD_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

   /**
    * System property key to define the fully qualified class name of default socket factory to use
    * when not using custom config.
    */
   public static final String REMOTING_DEFAULT_SOCKET_FACTORY_CLASS = "org.jboss.remoting.defaultSocketFactory";
   
   public static final String NONE_STORE = "NONE";

   private SSLContext sslContextServerSocketFactory = null; // context that builds the server socket factories
   private SSLContext sslContextSocketFactory = null; // context that builds the socket factories
   private Provider provider = null;
   private String providerName = null;
   private String secureSocketProtocol = null;

   private KeyManager[] keyManagers = null;
   private TrustManager[] trustManagers = null;
   private SecureRandom secureRandom = null;

   private URL    keyStoreFilePath = null;
   private String keyStoreType = null;
   private String keyStoreAlgorithm = null;
   private String keyStorePassword = null;
   private String keyAlias = null;
   private String keyPassword = null;

   private URL    trustStoreFilePath = null;
   private String trustStoreType = null;
   private String trustStoreAlgorithm = null;
   private String trustStorePassword = null;

   private Map config = null;
   private Boolean socketUseClientMode = null;
   private Boolean serverSocketUseClientMode = null;
   private String clientAuthMode = null;
   private Boolean serverAuthMode = null;

   private boolean useSSLServerSocketFactory = true;
   private boolean useSSLSocketFactory = true;

   private static final Logger log = Logger.getLogger(SSLSocketBuilder.class);
   
   private static URL NONE_STORE_URL;
   
   static
   {
      try
      {
         NONE_STORE_URL = new URL("file:NONE");
      } catch (MalformedURLException e)
      {
         log.info("unexpected URL exception", e);
      }
   }

   /**
    * Constructor for {@link SSLSocketBuilder} that does not have
    * any configuration so it falls back to all defaults.
    */
   public SSLSocketBuilder()
   {
      this(null);
   }

   /**
    * Constructor for {@link SSLSocketBuilder} that allows the caller to
    * override the default settings for the various SSL configuration
    * properties.
    *
    * @param config configuration with properties defining things like where the
    *               keystore and truststore files are, their types, etc.
    */
   public SSLSocketBuilder(Map config)
   {
      this.config = config;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setUseSSLServerSocketFactory(boolean)
    */
   public void setUseSSLServerSocketFactory(boolean shouldUse)
   {
      this.useSSLServerSocketFactory = shouldUse;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getUseSSLServerSocketFactory()
    */
   public boolean getUseSSLServerSocketFactory()
   {
      return useSSLServerSocketFactory;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setUseSSLSocketFactory(boolean)
    */
   public void setUseSSLSocketFactory(boolean shouldUse)
   {
      this.useSSLSocketFactory = shouldUse;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getUseSSLSocketFactory()
    */
   public boolean getUseSSLSocketFactory()
   {
      return useSSLSocketFactory;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#createSSLServerSocketFactory()
    */
   public ServerSocketFactory createSSLServerSocketFactory() throws IOException
   {
      return createSSLServerSocketFactory( null );
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#createSSLServerSocketFactory(org.jboss.remoting.security.CustomSSLServerSocketFactory)
    */
   public ServerSocketFactory createSSLServerSocketFactory(CustomSSLServerSocketFactory wrapper) throws IOException
   {
      ServerSocketFactory ssf = null;

      if( getUseSSLServerSocketFactory() )
      {
         ssf = SSLServerSocketFactory.getDefault();
      }
      else
      {
         if (wrapper == null)
         {
            wrapper = new CustomSSLServerSocketFactory(null, this);
         }

         ssf = createCustomServerSocketFactory(wrapper);
      }

      return ssf;
   }

   /**
    * This creates a fully custom SSL server socket factory using this object's configuration.
    *
    * @param wrapper the wrapper where the created factory will be stored
    *
    * @return the SSLServerSocketFactory
    *
    * @throws IOException
    */
   protected ServerSocketFactory createCustomServerSocketFactory(CustomSSLServerSocketFactory wrapper) throws IOException
   {
      if (sslContextServerSocketFactory == null)
      {
         createServerSocketFactorySSLContext();
         initializeServerSocketFactorySSLContext();
      }

      ServerSocketFactory ssf = sslContextServerSocketFactory.getServerSocketFactory();

      wrapper.setFactory((SSLServerSocketFactory) ssf);

      return wrapper;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#createSSLSocketFactory()
    */
   public SocketFactory createSSLSocketFactory() throws IOException
   {
      return createSSLSocketFactory(null);
   }


   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#createSSLSocketFactory(org.jboss.remoting.security.CustomSSLSocketFactory)
    */
   public SocketFactory createSSLSocketFactory(CustomSSLSocketFactory wrapper) throws IOException
   {
      SocketFactory sf = null;

      if (getUseSSLSocketFactory())
      {
         String defaultFactoryName = getSystemProperty(REMOTING_DEFAULT_SOCKET_FACTORY_CLASS);
         
         if (defaultFactoryName != null)
         {
            try
            {
               final Class sfClass = ClassLoaderUtility.loadClass(defaultFactoryName, SSLSocketBuilder.class);
               Method m = getMethod(sfClass, "getDefault", null);
               
               if (m == null)
               {
                  throw new RuntimeException(
                        "Could not create the socket factory "
                        + defaultFactoryName
                        + " because the class "
                        + sfClass
                        + " doesn't provide the getDefault method.");
               }
               sf = (SocketFactory) m.invoke(null, null);
            }
            catch (Exception ex)
            {
               throw new RuntimeException(
                     "Could not create the socket factory "
                     + defaultFactoryName, ex);
            }
         }
         if (sf == null)
         {
            sf = SSLSocketFactory.getDefault();
         }
      }
      else
      {
         if (wrapper == null)
         {
            wrapper = new CustomSSLSocketFactory(null, this);
         }

         sf = createCustomSocketFactory(wrapper);
      }

      return sf;
   }

   /**
    * This creates a fully custom SSL socket factory using this object's configuration.
    *
    * @param wrapper the wrapper where the created factory will be stored
    *
    * @return the wrapper with the new factory stored in it
    *
    * @throws IOException
    */
   protected SocketFactory createCustomSocketFactory(CustomSSLSocketFactory wrapper) throws IOException
   {
      if (sslContextSocketFactory == null)
      {
         createSocketFactorySSLContext();
         initializeSocketFactorySSLContext();
      }

      SocketFactory sf = sslContextSocketFactory.getSocketFactory();

      wrapper.setFactory((SSLSocketFactory) sf);

      return wrapper;
   }


   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getServerSocketFactorySSLContext()
    */
   public SSLContext getServerSocketFactorySSLContext()
   {
      return sslContextServerSocketFactory;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getSocketFactorySSLContext()
    */
   public SSLContext getSocketFactorySSLContext()
   {
      return sslContextSocketFactory;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getSecureSocketProtocol()
    */
   public String getSecureSocketProtocol()
   {
      if (secureSocketProtocol == null)
      {
         if(config != null)
         {
            secureSocketProtocol = (String) config.get(REMOTING_SSL_PROTOCOL);
         }
         if (secureSocketProtocol == null)
         {
            secureSocketProtocol = DEFAULT_SSL_PROTOCOL;
         }
      }

      return secureSocketProtocol;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setSecureSocketProtocol(String)
    */
   public void setSecureSocketProtocol(String protocol)
   {
      if(protocol != null && protocol.length() > 0)
      {
         this.secureSocketProtocol = protocol;
      }
      else
      {
         throw new IllegalArgumentException("Can not set remoting socket factory with null protocol");
      }
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getProvider()
    */
   public Provider getProvider()
   {
      return provider;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setProvider(java.security.Provider)
    */
   public void setProvider(Provider provider)
   {
      this.provider = provider;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getProviderName()
    */
   public String getProviderName()
   {
      if (providerName == null)
      {
         if(config != null)
         {
            providerName = (String) config.get(REMOTING_SSL_PROVIDER_NAME);
         }
      }
      return providerName;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setProviderName(java.lang.String)
    */
   public void setProviderName(String providerName)
   {
      this.providerName = providerName;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getSecureRandom()
    */
   public SecureRandom getSecureRandom()
   {
      if(secureRandom != null)
      {
         return secureRandom;
      }

      secureRandom = new SecureRandom();

      return secureRandom;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setSecureRandom(java.security.SecureRandom)
    */
   public void setSecureRandom(SecureRandom secureRandom)
   {
      this.secureRandom = secureRandom;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getKeyStoreURL()
    */
   public String getKeyStoreURL()
   {
      URL keyStore = getKeyStore();
      if(keyStore != null)
      {
         return keyStore.toString();
      }
      else
      {
         return null;
      }
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getKeyStore()
    */
   public URL getKeyStore()
   {
      if(keyStoreFilePath != null)
      {
         return keyStoreFilePath;
      }

      if(config != null)
      {
         String path = (String) config.get(REMOTING_KEY_STORE_FILE_PATH);
         if(path != null && path.length() > 0)
         {
            setKeyStoreURL( path );
         }
      }

      if(keyStoreFilePath == null)
      {
         String path = getSystemProperty(STANDARD_KEY_STORE_FILE_PATH);;
         if(path != null && path.length() > 0)
         {
            setKeyStoreURL( path );
         }
      }

      return keyStoreFilePath;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyStoreURL(java.lang.String)
    */
   public void setKeyStoreURL(String keyStoreFilePath)
   {
      try
      {
         this.keyStoreFilePath = validateStoreURL(keyStoreFilePath);
      }
      catch (IOException e)
      {
         throw new RuntimeException( "Cannot validate the store URL: " + keyStoreFilePath , e );
      }
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyStore(java.net.URL)
    */
   public void setKeyStore(URL keyStore)
   {
      this.keyStoreFilePath = keyStore;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getKeyStoreType()
    */
   public String getKeyStoreType()
   {
      if(keyStoreType != null)
      {
         return keyStoreType;
      }

      if(config != null)
      {
         String type = (String)config.get(REMOTING_KEY_STORE_TYPE);
         if(type != null && type.length() > 0)
         {
            keyStoreType = type;
         }
      }

      if(keyStoreType == null)
      {
         keyStoreType = getSystemProperty(STANDARD_KEY_STORE_TYPE);
         if(keyStoreType == null)
         {
            keyStoreType = DEFAULT_KEY_STORE_TYPE;
         }
      }

      return keyStoreType;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyStoreType(java.lang.String)
    */
   public void setKeyStoreType(String keyStoreType)
   {
      this.keyStoreType = keyStoreType;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getKeyStoreAlgorithm()
    */
   public String getKeyStoreAlgorithm()
   {
      if(keyStoreAlgorithm != null)
      {
         return keyStoreAlgorithm;
      }

      if(config != null)
      {
         String alg = (String)config.get(REMOTING_KEY_STORE_ALGORITHM);
         if(alg != null && alg.length() > 0)
         {
            keyStoreAlgorithm = alg;
         }
      }

      if(keyStoreAlgorithm == null)
      {
         keyStoreAlgorithm = DEFAULT_KEY_STORE_ALGORITHM;
      }

      return keyStoreAlgorithm;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyStoreAlgorithm(java.lang.String)
    */
   public void setKeyStoreAlgorithm(String algorithm)
   {
      this.keyStoreAlgorithm = algorithm;
   }

   /**
    * Returns the password used to gain access to the keystore.
    *
    * @return keystore password
    */
   public String getKeyStorePassword()
   {
      if(keyStorePassword != null)
      {
         return keyStorePassword;
      }

      if(config != null)
      {
         String passwd = (String)config.get(REMOTING_KEY_STORE_PASSWORD);
         if(passwd != null && passwd.length() > 0)
         {
            keyStorePassword = passwd;
         }
      }

      if(keyStorePassword == null)
      {
         keyStorePassword = getSystemProperty(STANDARD_KEY_STORE_PASSWORD);
      }

      return keyStorePassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyStorePassword(java.lang.String)
    */
   public void setKeyStorePassword(String keyStorePassword)
   {
      this.keyStorePassword = keyStorePassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getTrustStoreURL()
    */
   public String getTrustStoreURL()
   {
      URL trustStore = getTrustStore();
      if(trustStore != null)
      {
         return trustStore.toString();
      }
      else
      {
         return null;
      }
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getTrustStore()
    */
   public URL getTrustStore()
   {
      if(trustStoreFilePath != null)
      {
         return trustStoreFilePath;
      }

      if(config != null)
      {
         String path = (String)config.get(REMOTING_TRUST_STORE_FILE_PATH);
         if(path != null && path.length() > 0)
         {
            setTrustStoreURL( path );
         }
      }

      if(trustStoreFilePath == null)
      {
         String path = getSystemProperty(STANDARD_TRUST_STORE_FILE_PATH);
         if(path != null && path.length() > 0)
         {
            setTrustStoreURL( path );
         }
      }

      return trustStoreFilePath;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setTrustStore(java.net.URL)
    */
   public void setTrustStore(URL trustStore)
   {
      this.trustStoreFilePath = trustStore;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setTrustStoreURL(java.lang.String)
    */
   public void setTrustStoreURL(String trustStoreFilePath)
   {
      try
      {
         this.trustStoreFilePath = validateStoreURL(trustStoreFilePath);
      }
      catch (IOException e)
      {
         throw new RuntimeException( "Cannot validate the store URL: " + trustStoreFilePath , e );
      }
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getTrustStoreType()
    */
   public String getTrustStoreType()
   {
      if(trustStoreType != null)
      {
         return trustStoreType;
      }

      if(config != null)
      {
         String type = (String)config.get(REMOTING_TRUST_STORE_TYPE);
         if(type != null && type.length() > 0)
         {
            trustStoreType = type;
         }
      }

      if(trustStoreType == null)
      {
         trustStoreType = getSystemProperty(STANDARD_TRUST_STORE_TYPE);
         if(trustStoreType == null)
         {
            trustStoreType = getKeyStoreType();
         }
      }

      return trustStoreType;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setTrustStoreType(java.lang.String)
    */
   public void setTrustStoreType(String trustStoreType)
   {
      this.trustStoreType = trustStoreType;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getTrustStoreAlgorithm()
    */
   public String getTrustStoreAlgorithm()
   {
      if(trustStoreAlgorithm != null)
      {
         return trustStoreAlgorithm;
      }

      if(config != null)
      {
         String alg = (String)config.get(REMOTING_TRUST_STORE_ALGORITHM);
         if(alg != null && alg.length() > 0)
         {
            trustStoreAlgorithm = alg;
         }
      }

      if(trustStoreAlgorithm == null)
      {
         trustStoreAlgorithm = getKeyStoreAlgorithm();
      }

      return trustStoreAlgorithm;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setTrustStoreAlgorithm(java.lang.String)
    */
   public void setTrustStoreAlgorithm(String algorithm)
   {
      this.trustStoreAlgorithm = algorithm;
   }

   /**
    * Returns the password used to gain access to the truststore.
    *
    * @return truststore password
    */
   public String getTrustStorePassword()
   {
      if(trustStorePassword != null)
      {
         return trustStorePassword;
      }

      if(config != null)
      {
         String passwd = (String)config.get(REMOTING_TRUST_STORE_PASSWORD);
         if(passwd != null && passwd.length() > 0)
         {
            trustStorePassword = passwd;
         }
      }

      if(trustStorePassword == null)
      {
         trustStorePassword = getSystemProperty(STANDARD_TRUST_STORE_PASSWORD);
         if(trustStorePassword == null)
         {
            trustStorePassword = getKeyStorePassword();
         }
      }

      return trustStorePassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setTrustStorePassword(java.lang.String)
    */
   public void setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword = trustStorePassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getKeyAlias()
    */
   public String getKeyAlias()
   {
      if(keyAlias != null)
      {
         return keyAlias;
      }
      if(config != null)
      {
         keyAlias = (String)config.get(REMOTING_KEY_ALIAS);
      }
      return keyAlias;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyAlias(java.lang.String)
    */
   public void setKeyAlias(String alias)
   {
      this.keyAlias = alias;
   }

   /**
    * Returns the password to use for the keys within the key store.
    * If this value is not set, this will return <code>null</code> but
    * when this value is needed by this class, the value for the key store
    * password will be used instead.
    *
    * @return key password
    */
   public String getKeyPassword()
   {
      if(keyPassword != null)
      {
         return keyPassword;
      }

      if(config != null)
      {
         String passwd = (String)config.get(REMOTING_KEY_PASSWORD);
         if(passwd != null && passwd.length() > 0)
         {
            keyPassword = passwd;
         }
      }

      return keyPassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setKeyPassword(java.lang.String)
    */
   public void setKeyPassword(String keyPassword)
   {
      this.keyPassword = keyPassword;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isSocketUseClientMode()
    */
   public boolean isSocketUseClientMode( )
   {
      if (socketUseClientMode == null)
      {
         if (config != null && config.containsKey(REMOTING_SOCKET_USE_CLIENT_MODE))
         {
            socketUseClientMode = Boolean.valueOf((String) config.get(REMOTING_SOCKET_USE_CLIENT_MODE));
         }
         else
         {
            socketUseClientMode = Boolean.TRUE;
         }
      }

      return socketUseClientMode.booleanValue();
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isServerSocketUseClientMode()
    */
   public boolean isServerSocketUseClientMode( )
   {
      if (serverSocketUseClientMode == null)
      {
         if (config != null && config.containsKey(REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
         {
            serverSocketUseClientMode = Boolean.valueOf((String) config.get(REMOTING_SERVER_SOCKET_USE_CLIENT_MODE));
         }
         else
         {
            serverSocketUseClientMode = Boolean.FALSE;
         }
      }

      return serverSocketUseClientMode.booleanValue();
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setSocketUseClientMode(boolean)
    */
   public void setSocketUseClientMode( boolean useClientMode )
   {
      this.socketUseClientMode = Boolean.valueOf(useClientMode);
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setServerSocketUseClientMode(boolean)
    */
   public void setServerSocketUseClientMode( boolean useClientMode )
   {
      this.serverSocketUseClientMode = Boolean.valueOf(useClientMode);
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isClientAuthModeNone()
    */
   public boolean isClientAuthModeNone()
   {
      return CLIENT_AUTH_MODE_NONE.equals(getClientAuthMode());
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isClientAuthModeWant()
    */
   public boolean isClientAuthModeWant()
   {
      return CLIENT_AUTH_MODE_WANT.equals(getClientAuthMode());
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isClientAuthModeNeed()
    */
   public boolean isClientAuthModeNeed()
   {
      return CLIENT_AUTH_MODE_NEED.equals(getClientAuthMode());
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#getClientAuthMode()
    */
   public String getClientAuthMode()
   {
      if (clientAuthMode == null)
      {
         if (config != null && config.containsKey(REMOTING_CLIENT_AUTH_MODE))
         {
            setClientAuthMode( (String) config.get(REMOTING_CLIENT_AUTH_MODE) );
         }
         else
         {
            clientAuthMode = CLIENT_AUTH_MODE_NONE;
         }
      }

      return clientAuthMode;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setClientAuthMode(java.lang.String)
    */
   public void setClientAuthMode(String mode)
   {
      if (  mode == null ||
            (!mode.equalsIgnoreCase(CLIENT_AUTH_MODE_NONE)
             && !mode.equalsIgnoreCase(CLIENT_AUTH_MODE_WANT)
             && !mode.equalsIgnoreCase(CLIENT_AUTH_MODE_NEED)))
      {
         log.warn("Client authentication mode is invalid [" + mode + "]; falling back to NEED mode");
         clientAuthMode = CLIENT_AUTH_MODE_NEED;
      }
      else
      {
         clientAuthMode = mode;
      }

      return;
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#isServerAuthMode()
    */
   public boolean isServerAuthMode()
   {
      if (serverAuthMode == null)
      {
         if (config != null && config.containsKey(REMOTING_SERVER_AUTH_MODE))
         {
            serverAuthMode = Boolean.valueOf( (String) config.get(REMOTING_SERVER_AUTH_MODE) );
         }
         else
         {
            serverAuthMode = Boolean.TRUE;
         }
      }

      return serverAuthMode.booleanValue();
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#setServerAuthMode(boolean)
    */
   public void setServerAuthMode(boolean mode)
   {
      serverAuthMode = Boolean.valueOf(mode);
   }

   /**
    * Creates (but does not initialize) the SSL context used by this object
    * to create server socket factories.
    * The provider/protocol is used to determine what SSL context to use.
    * Call {@link #initializeServerSocketFactorySSLContext()} if you want
    * to create and initialize in one method call.
    * If the server socket factory SSL context was already created, this will create
    * a new one and remove the old one.
    *
    * @throws IOException
    */
   protected void createServerSocketFactorySSLContext()
         throws IOException
   {
      try
      {
         if(getProvider() != null)
         {
            sslContextServerSocketFactory = SSLContext.getInstance(getSecureSocketProtocol(), getProvider());
         }
         else if(getProviderName() != null)
         {
            sslContextServerSocketFactory = SSLContext.getInstance(getSecureSocketProtocol(), getProviderName());
         }
         else
         {
            sslContextServerSocketFactory = SSLContext.getInstance(getSecureSocketProtocol());
         }
      }
      catch(Exception e)
      {
         IOException ioe = new IOException("Error creating server socket factory SSL context: " + e.getMessage());
         ioe.setStackTrace(e.getStackTrace());
         throw ioe;
      }

      return;
   }

   /**
    * Creates (but does not initialize) the SSL context used by this object
    * to create socket factories.
    * The provider/protocol is used to determine what SSL context to use.
    * Call {@link #initializeSocketFactorySSLContext()} if you want
    * to create and initialize in one method call.
    * If the socket factory SSL context was already created, this will create
    * a new one and remove the old one.
    *
    * @throws IOException
    */
   protected void createSocketFactorySSLContext()
   throws IOException
   {
      try
      {
         if(getProvider() != null)
         {
            sslContextSocketFactory = SSLContext.getInstance(getSecureSocketProtocol(), getProvider());
         }
         else if(getProviderName() != null)
         {
            sslContextSocketFactory = SSLContext.getInstance(getSecureSocketProtocol(), getProviderName());
         }
         else
         {
            sslContextSocketFactory = SSLContext.getInstance(getSecureSocketProtocol());
         }
      }
      catch(Exception e)
      {
         IOException ioe = new IOException("Error creating socket factory SSL context: " + e.getMessage());
         ioe.setStackTrace(e.getStackTrace());
         throw ioe;
      }

      return;
   }

   /**
    * Initializes the SSL context used by this object that will create the server socket factories.
    * If the SSL context is not yet created, this method will also create it.
    * The provider/protocol is used to determine what SSL context to use.  Key and trust managers
    * are loaded and a secure random object is created and the SSL context for the
    * protocol/provider is initialized with them.
    *
    * @throws IOException
    */
   protected void initializeServerSocketFactorySSLContext()
         throws IOException
   {
      try
      {
         if (sslContextServerSocketFactory == null)
         {
            createServerSocketFactorySSLContext();
         }

         try
         {
            keyManagers = loadKeyManagers();
         }
         catch (NullStoreURLException e)
         {
            if (isServerSocketUseClientMode())
            {
               keyManagers = null;
               log.debug("Could not find keytore url.  " + e.getMessage());
            }
            else
            {
               // because this ssl context will create server socket factories, will throw if can not find keystore
               IOException ioe = new IOException("Can not find keystore url.");
               ioe.initCause(e);
               throw ioe;
            }
         }

         try
         {
            boolean isClientMode = isServerSocketUseClientMode();
            trustManagers = loadTrustManagers(isClientMode);
         }
         catch (NullStoreURLException e)
         {
            trustManagers = null;
            log.debug("Could not find truststore url.  " + e.getMessage());
         }

         secureRandom = getSecureRandom();

         sslContextServerSocketFactory.init(keyManagers, trustManagers, secureRandom);
      }
      catch(Exception e)
      {
         IOException ioe = new IOException("Error initializing server socket factory SSL context: " + e.getMessage());
         ioe.setStackTrace(e.getStackTrace());
         throw ioe;
      }

      return;
   }

   /**
    * Initializes the SSL context used by this object that will create the socket factories.
    * If the SSL context is not yet created, this method will also create it.
    * The provider/protocol is used to determine what SSL context to use.  Key and trust managers
    * are loaded and a secure random object is created and the SSL context for the
    * protocol/provider is initialized with them.
    *
    * @throws IOException
    */
   protected void initializeSocketFactorySSLContext()
   throws IOException
   {
      try
      {
         if (sslContextSocketFactory == null)
         {
            createSocketFactorySSLContext();
         }

         try
         {
            keyManagers = loadKeyManagers();
         }
         catch (NullStoreURLException e)
         {
            if (isSocketUseClientMode())
            {
               // this is allowable since would be the normal scenario
               keyManagers = null;
               log.debug("Could not find keystore url.  " + e.getMessage());
            }
            else
            {
               IOException ioe = new IOException("Can not find keystore url.");
               ioe.initCause(e);
               throw ioe;
            }
         }

         try
         {
            boolean isClientMode = isSocketUseClientMode();
            trustManagers = loadTrustManagers(isClientMode);
         }
         catch (NullStoreURLException e)
         {
            // If the keyManagers is not null, could possibly be using in client mode
            // so want to allow it.  Otherwise, need to throw exception as will not be able
            // to use in client mode or not
            if(keyManagers != null)
            {
               trustManagers = null;
               log.debug("Could not find truststore url.  " + e.getMessage());
            }
            else
            {
               IOException ioe = new IOException("Can not find truststore url.");
               ioe.initCause(e);
               throw ioe;
            }
         }

         secureRandom = getSecureRandom();

         sslContextSocketFactory.init(keyManagers, trustManagers, secureRandom);
      }
      catch(Exception e)
      {
         IOException ioe = new IOException("Error initializing socket factory SSL context: " + e.getMessage());
         ioe.setStackTrace(e.getStackTrace());
         throw ioe;
      }

      return;
   }

   /**
    * Loads the trust managers based on this object's truststore.
    *
    * @return array of trust managers that should be loaded in this object's SSL context
    *
    * @throws NoSuchProviderException
    * @throws NoSuchAlgorithmException
    * @throws IOException
    * @throws CertificateException
    * @throws KeyStoreException
    * @throws NullStoreURLException
    */
   protected TrustManager[] loadTrustManagers(boolean isClientMode)
         throws NoSuchProviderException, NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, NullStoreURLException
   {
      if(isClientMode && !isServerAuthMode())
      {
         // we are in client mode and do not want to perform server cert authentication
         // return a trust manager that trusts all certs
         trustManagers = new TrustManager[] {
               new X509TrustManager() {
                  public void checkClientTrusted( X509Certificate[] chain, String authType ) {}
                  public void checkServerTrusted( X509Certificate[] chain, String authType ) {}
                  public X509Certificate[] getAcceptedIssuers()  { return null; }
               }};
      }
      else
      {
         String tsType = getTrustStoreType();
         String tsPasswd = getTrustStorePassword();
         URL tsPathURL = getTrustStore();

         String tsAlg = getTrustStoreAlgorithm();

         TrustManagerFactory trustMgrFactory;
         KeyStore trustStore = loadKeyStore(tsType, tsPathURL, tsPasswd);

         if (getProvider() != null)
         {
            trustMgrFactory = TrustManagerFactory.getInstance(tsAlg, getProvider());
         }
         else if (getProviderName() != null)
         {
            trustMgrFactory = TrustManagerFactory.getInstance(tsAlg, getProviderName());
         }
         else
         {
            trustMgrFactory = TrustManagerFactory.getInstance(tsAlg);
         }

         if (trustStore != null)
         {
            trustMgrFactory.init(trustStore);

            trustManagers = trustMgrFactory.getTrustManagers();
         }
      }

      return trustManagers;
   }

   /**
    * Loads the key managers based on this object's truststore.
    *
    * @return array of key managers that should be loaded in this object's SSL context
    *
    * @throws NoSuchProviderException
    * @throws NoSuchAlgorithmException
    * @throws IOException
    * @throws CertificateException
    * @throws KeyStoreException
    * @throws UnrecoverableKeyException
    * @throws NullStoreURLException
    */
   protected KeyManager[] loadKeyManagers()
         throws NoSuchProviderException, NoSuchAlgorithmException, IOException, CertificateException,
                KeyStoreException, UnrecoverableKeyException, NullStoreURLException
   {
      String ksPasswd = getKeyStorePassword();
      String ksType = getKeyStoreType();
      URL ksPathURL = getKeyStore();

      KeyStore keyStore = loadKeyStore(ksType, ksPathURL, ksPasswd);

      if(keyStore != null)
      {
         String alias = getKeyAlias();

         // check that keystore contains supplied alias (if there is one)
         if(alias != null)
         {
            boolean containsAlias = keyStore.isKeyEntry(alias);
            if(!containsAlias)
            {
               // can not continue as supplied alias does not exist as key entry
               throw new IOException("Can not find key entry for key store (" + ksPathURL + ") with given alias (" + alias + ")");
            }
         }

         KeyManagerFactory keyMgrFactory = null;
         String alg = getKeyStoreAlgorithm();

         if(getProvider() != null)
         {
            keyMgrFactory = KeyManagerFactory.getInstance(alg, getProvider());
         }
         else if(getProviderName() != null)
         {
            keyMgrFactory = KeyManagerFactory.getInstance(alg, getProviderName());
         }
         else
         {
            keyMgrFactory = KeyManagerFactory.getInstance(alg);
         }

         // get they key password, if it isn't defined, use the key store password
         String keyPasswd = getKeyPassword();
         if (keyPasswd == null || keyPasswd.length() == 0)
         {
            keyPasswd = ksPasswd;
         }

         char[] keyPasswdCharArray = keyPasswd == null ? null : keyPasswd.toCharArray();
         keyMgrFactory.init(keyStore, keyPasswdCharArray);
         keyManagers = keyMgrFactory.getKeyManagers();

         // if alias provided, use helper impl to hard wire alias name to be used
         if(alias != null)
         {
            //TODO: -TME Need careful review of if this is really needed or not.
            for(int x = 0; x < keyManagers.length; x++)
            {
               keyManagers[x] = new RemotingKeyManager((X509KeyManager)keyManagers[x], alias);
            }
         }

      }
      return keyManagers;
   }

   /**
    * Loads a key store file and returns it.
    *
    * @param storeType the type of store file
    * @param storePathURL the URL to the file - may be relative to the current thread's classloader
    *                  or may be absolute path to a file on the file system.
    * @param storePassword password to gain access to the store file
    *
    * @return the key store
    *
    * @throws KeyStoreException
    * @throws NoSuchProviderException
    * @throws IOException
    * @throws NoSuchAlgorithmException
    * @throws CertificateException
    * @throws NullStoreURLException
    */
   protected KeyStore loadKeyStore(String storeType, URL storePathURL, String storePassword)
         throws KeyStoreException, NoSuchProviderException, IOException, NoSuchAlgorithmException, CertificateException, NullStoreURLException
   {
      KeyStore keyStore = null;

      if(getProvider() != null)
      {
         keyStore = KeyStore.getInstance(storeType, getProvider());
      }
      else if(getProviderName() != null)
      {
         keyStore = KeyStore.getInstance(storeType, getProviderName());
      }
      else
      {
         keyStore = KeyStore.getInstance(storeType);
      }

      if ( storePathURL == null )
      {
         throw new NullStoreURLException("Can not find store file for url because store url is null.");
      }

      URL url = storePathURL == NONE_STORE_URL ? null : storePathURL;
      
      // now that keystore instance created, need to load data from file
      InputStream keyStoreInputStream = null;

      try
      {
         if (url != null)
         {
            keyStoreInputStream = url.openStream();
         }
      	
         // is ok for password to be null, as will just be used to check integrity of store
         char[] password = storePassword != null ? storePassword.toCharArray() : null;
         keyStore.load(keyStoreInputStream, password);
      }
      finally
      {
         if(keyStoreInputStream != null)
         {
            try
            {
               keyStoreInputStream.close();
            }
            catch(IOException e)
            {
               // no op
            }
            keyStoreInputStream = null;
         }
      }

      return keyStore;
   }

   /**
    * Given a store file path, this will verify that the store actually exists.
    * First, it checks to see if its a valid URL, then it checks to see if the
    * file path is found in the file system and finally will be checked to see
    * if it can be found as a resource within the current thread's classloader.
    * An exception is thrown if the store cannot be found.
    *
    * @param storePath the path which can be a URL, path to a resource in classloader
    *                  or a file path on the file system.
    *
    * @return the URL of the file that was found
    *
    * @throws IOException if the store could not be found
    */
   protected URL validateStoreURL(String storePath) throws IOException
   {
      if (NONE_STORE.equals(storePath))
      {
         return NONE_STORE_URL;
      }
      
      URL url = null;

      // First see if this is a URL
      try
      {
         url = new URL(storePath);
      }
      catch(MalformedURLException e)
      {
         // Not a URL or a protocol without a handler so...
         // next try to locate this as file path
         final File tst = new File(storePath);
         Boolean exists = (Boolean) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return new Boolean(tst.exists());
            }
         });

         if(exists.booleanValue())
         {
            url = tst.toURL();
         }
         else
         {
            // not a file either, lastly try to locate this as a classpath resource
            if(url == null)
            {
               ClassLoader loader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
               {
                  public Object run()
                  {
                     return Thread.currentThread().getContextClassLoader();
                  }
               });
               url = loader.getResource(storePath);
            }
         }
      }

      // Fail if no valid key store was located
      if(url == null)
      {
         String msg = "Failed to find url=" + storePath + " as a URL, file or resource";
         throw new MalformedURLException(msg);
      }

      return url;
   }

   public Object clone()
   {
      try
      {
         return super.clone();
      }
      catch (CloneNotSupportedException e)
      {
         return null;
      }
   }

   //*******************************************************************
   // START
   // The following are just needed in order to make it a service mbean.
   // They are just NOOPs (no implementation).
   //*******************************************************************

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#create()
    */
   public void create() throws Exception
   {
      return; // no-op
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#start()
    */
   public void start() throws Exception
   {
      return; // no-op
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#stop()
    */
   public void stop()
   {
      return; // no-op
   }

   /**
    * @see org.jboss.remoting.security.SSLSocketBuilderMBean#destroy()
    */
   public void destroy()
   {
      return; // no-op
   }

   //*******************************************************************
   // END
   //*******************************************************************

   /**
    * Used to indicate a store URL was not specified and thus the store is not available.
    */
   protected class NullStoreURLException extends Exception
   {
      private static final long serialVersionUID = 1L;

      /**
       * @see Exception#Exception(String)
       */
      public NullStoreURLException(String message)
      {
         super(message);
      }
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private Method getMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return c.getMethod(name, parameterTypes);
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               return c.getMethod(name, parameterTypes);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
}