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

package org.jboss.remoting.transport.http.ssl;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.security.CustomSSLSocketFactory;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketBuilderMBean;
import org.jboss.remoting.socketfactory.SocketFactoryWrapper;
import org.jboss.remoting.transport.http.HTTPClientInvoker;
import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSClientInvoker extends HTTPClientInvoker
{
   /**
    * A property to override the default https url host verification
    */
   public static final String IGNORE_HTTPS_HOST = "org.jboss.security.ignoreHttpsHost";
   public static final String HOSTNAME_VERIFIER = "hostnameVerifier";

   public HTTPSClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public HTTPSClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   protected String validateURL(String url)
   {
      String validatedUrl = url;

      if (validatedUrl.startsWith("servlet"))
      {
         // servlet:// is a valid protocol, but only in the remoting world, so need to convert to http
         validatedUrl = "http" + validatedUrl.substring("servlet".length());
      }
      else if(validatedUrl.startsWith("sslservlet"))
      {
         // sslservlet:// is a valid protocol, but only in the remoting world, so need to convert to https
         validatedUrl = "https" + validatedUrl.substring("sslservlet".length());
      }
      return validatedUrl;
   }


   /**
    * Checks to see if org.jboss.security.ignoreHttpHost property is set to true, and if it
    * is, will ste the host name verifier so that will accept any host.
    *
    * @return
    * @throws IOException
    */
   protected HttpURLConnection createURLConnection(String url, Map metadata) throws IOException
   {
      HttpURLConnection conn = super.createURLConnection(url, metadata);

      if (conn instanceof HttpsURLConnection)
      {
         HttpsURLConnection sconn = (HttpsURLConnection) conn;

         SocketFactory socketFactory = getSocketFactory();
         if (socketFactory != null && socketFactory instanceof SSLSocketFactory)
         {
            SSLSocketFactory sslSocketFactory = getHandshakeCompatibleFactory((SSLSocketFactory) socketFactory, metadata);
            sconn.setSSLSocketFactory(sslSocketFactory);
         }

         setHostnameVerifier(sconn, metadata);
      }

      return conn;
   }

   private SSLSocketFactory getHandshakeCompatibleFactory(SSLSocketFactory socketFactory, Map metadata)
   {
      SSLSocketFactory sslSocketFactory = socketFactory;

      // need to check for handshake listener and add them if there is one
      Object obj = configuration.get(Client.HANDSHAKE_COMPLETED_LISTENER);
      if (obj != null && obj instanceof HandshakeCompletedListener)
      {
         HandshakeCompletedListener listener = (HandshakeCompletedListener) obj;
         sslSocketFactory = new HTTPSSocketFactory(socketFactory, listener);
      }
      return sslSocketFactory;
   }


   protected SocketFactory createSocketFactory(Map configuration)
   {
      SocketFactory sf = super.createSocketFactory(configuration);
      
      if (isCompleteSocketFactory(sf))
         return sf;
      
      SocketFactory wrapper = sf;

      try
      {
         SSLSocketBuilder server = new SSLSocketBuilder(configuration);
         server.setUseSSLSocketFactory(false);
         sf = server.createSSLSocketFactory();
      }
      catch (Exception e)
      {
         log.error("Error creating SSL Socket Factory for client invoker: " + e.getMessage());
         log.debug("Error creating SSL Socket Factory for client invoker.", e);
      }

      if (wrapper != null)
      {
         ((SocketFactoryWrapper) wrapper).setSocketFactory(sf);
         return wrapper;
      }
      
      return sf;
   }

   protected void setHostnameVerifier(HttpsURLConnection conn, Map metadata)
   {
      HostnameVerifier hostnameVerifier = null;

      // First look for specific HostnameVerifier classname.
      String hostnameVerifierString = (String)metadata.get(HOSTNAME_VERIFIER);
      if (hostnameVerifierString == null || hostnameVerifierString.length() == 0)
         hostnameVerifierString = (String)configuration.get(HOSTNAME_VERIFIER);
      if(hostnameVerifierString != null && hostnameVerifierString.length() > 0)
      {
         try
         {
            Class cl = ClassLoaderUtility.loadClass(hostnameVerifierString, getClass());
            Constructor constructor = cl.getConstructor(new Class[]{});
            hostnameVerifier = (HostnameVerifier) constructor.newInstance(new Object[] {});
            log.trace("HostnameVerifier (" + hostnameVerifierString + ") loaded");
         }
         catch(Exception e)
         {
            log.debug("Could not create server socket factory by classname (" + hostnameVerifierString + ").  Error message: " + e.getMessage());
         }
      }

      // If we still don't have a HostnameVerifier, look for directive to ignore host name.
      if (hostnameVerifier == null)
      {  
         Boolean b = (Boolean) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return new Boolean(Boolean.getBoolean(IGNORE_HTTPS_HOST));
            }
         });
         
         boolean ignoreHTTPSHost = b.booleanValue();
         String ignoreHost = (String) metadata.get(IGNORE_HTTPS_HOST);
         if (ignoreHost != null && ignoreHost.length() > 0)
         {
            ignoreHTTPSHost = Boolean.valueOf(ignoreHost).booleanValue();
         }
         else
         {
            ignoreHost = (String) configuration.get(IGNORE_HTTPS_HOST);
            if (ignoreHost != null && ignoreHost.length() > 0)
            {
               ignoreHTTPSHost = Boolean.valueOf(ignoreHost).booleanValue();
            }
         }
         if (ignoreHTTPSHost)
         {
            hostnameVerifier = new AnyhostVerifier();
         }
      }

      // If we still don't have a HostnameVerifier, see if the SocketFactory is an instance of
      // org.jboss.remoting.security.CustomSSLSocketFactory, and, if so, if it has turned off
      // authentication.
      if (hostnameVerifier == null)
      {
         if (getSocketFactory() instanceof CustomSSLSocketFactory)
         {
            CustomSSLSocketFactory sf = (CustomSSLSocketFactory) getSocketFactory();
            SSLSocketBuilderMBean builder = sf.getSSLSocketBuilder();
            if (( builder.isSocketUseClientMode() && !builder.isServerAuthMode())
             || (!builder.isSocketUseClientMode() &&  builder.isClientAuthModeNone()))
               hostnameVerifier = new AnyhostVerifier();
         }
      }

      if (hostnameVerifier != null)
         conn.setHostnameVerifier(hostnameVerifier);
   }

   protected class AnyhostVerifier implements HostnameVerifier
   {

      public boolean verify(String s, SSLSession sslSession)
      {
         return true;
      }
   }
}