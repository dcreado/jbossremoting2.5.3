/**
 * This class uses code taken directly from the org.apache.tomcat.util.net.SSLSupport class of the
 * Apache tomcat-connectors project.  Please refer to the NOTICE file included in this distribution for
 * more details.  The following is the copyright, patent, trademark, and attribution notices from the
 * SSLSupport source, which this class also maintains:
 *
 *  Copyright 1999-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *    @author EKR
 *    @author Craig R. McClanahan
 *    Parts cribbed from JSSECertCompat
 *    Parts cribbed from CertificatesValve
 *
 * (the full source of the org.apache.tomcat.util.net.SSLSupport can be found at
 * http://svn.apache.org/repos/asf/tomcat/connectors/trunk/util/java/org/apache/tomcat/util/net/jsse/JSSESupport.java).
 */
package org.jboss.remoting.transport.coyote.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;
import org.apache.tomcat.util.net.SSLSupport;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RemotingSSLSupport implements SSLSupport
{
   private SSLSocket sslSocket;
   private SSLSession session;

   public RemotingSSLSupport(SSLSocket socket)
   {
      this.sslSocket = socket;
      this.session = socket.getSession();
   }
   
   public RemotingSSLSupport(SSLSession session)
   {
      this.session = session;
   }

   /**
    * The cipher suite being used on this connection.
    */
   public String getCipherSuite() throws IOException
   {
      if(session == null)
      {
         return null;
      }
      return session.getCipherSuite();
   }

   /**
    * The client certificate chain (if any).
    */
   public Object[] getPeerCertificateChain() throws IOException
   {
      return getPeerCertificateChain(false);
   }

   public Object[] getPeerCertificateChain(boolean force)
         throws IOException
   {
      if(session == null)
      {
         return null;
      }

      // Convert JSSE's certificate format to the ones we need
      X509Certificate [] jsseCerts = null;
      try
      {
         jsseCerts = session.getPeerCertificateChain();
      }
      catch(Exception bex)
      {
         // ignore.
      }
      if(jsseCerts == null)
      {
         jsseCerts = new X509Certificate[0];
      }
      if(jsseCerts.length <= 0 && force)
      {
         session.invalidate();
         handShake();
         session = sslSocket.getSession();
      }
      return getX509Certificates(session);

   }

   protected void handShake() throws IOException
   {
      if (sslSocket != null)
      {
         sslSocket.setNeedClientAuth(true);
         sslSocket.startHandshake();
      }
   }

   protected java.security.cert.X509Certificate[] getX509Certificates(SSLSession session) throws IOException
   {
      X509Certificate jsseCerts[] = null;
      try
      {
         jsseCerts = session.getPeerCertificateChain();
      }
      catch(Throwable ex)
      {
         // Get rid of the warning in the logs when no Client-Cert is
         // available
      }

      if(jsseCerts == null)
      {
         jsseCerts = new X509Certificate[0];
      }
      java.security.cert.X509Certificate [] x509Certs = new java.security.cert.X509Certificate[jsseCerts.length];
      for(int i = 0; i < x509Certs.length; i++)
      {
         try
         {
            byte buffer[] = jsseCerts[i].getEncoded();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
            x509Certs[i] = (java.security.cert.X509Certificate) cf.generateCertificate(stream);
         }
         catch(Exception ex)
         {
            return null;
         }
      }

      if(x509Certs.length < 1)
      {
         return null;
      }
      return x509Certs;
   }

   /**
    * Get the keysize.
    * <p/>
    * What we're supposed to put here is ill-defined by the
    * Servlet spec (S 4.7 again). There are at least 4 potential
    * values that might go here:
    * <p/>
    * (a) The size of the encryption key
    * (b) The size of the MAC key
    * (c) The size of the key-exchange key
    * (d) The size of the signature key used by the server
    * <p/>
    * Unfortunately, all of these values are nonsensical.
    */
   public Integer getKeySize() throws IOException
   {
      SSLSupport.CipherData c_aux[] = ciphers;
      if(session == null)
      {
         return null;
      }
      Integer keySize = (Integer) session.getValue(KEY_SIZE_KEY);
      if(keySize == null)
      {
         int size = 0;
         String cipherSuite = session.getCipherSuite();
         for(int i = 0; i < c_aux.length; i++)
         {
            if(cipherSuite.indexOf(c_aux[i].phrase) >= 0)
            {
               size = c_aux[i].keySize;
               break;
            }
         }
         keySize = new Integer(size);
         session.putValue(KEY_SIZE_KEY, keySize);
      }
      return keySize;
   }

   /**
    * The current session Id.
    */
   public String getSessionId() throws IOException
   {
      if(session == null)
      {
         return null;
      }
      // Expose ssl_session (getId)
      byte [] ssl_session = session.getId();
      if(ssl_session == null)
      {
         return null;
      }
      StringBuffer buf = new StringBuffer("");
      for(int x = 0; x < ssl_session.length; x++)
      {
         String digit = Integer.toHexString((int) ssl_session[x]);
         if(digit.length() < 2)
         {
            buf.append('0');
         }
         if(digit.length() > 2)
         {
            digit = digit.substring(digit.length() - 2);
         }
         buf.append(digit);
      }
      return buf.toString();
   }
}