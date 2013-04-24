package org.jboss.test.remoting.transport.http.ssl.handshake;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.test.remoting.transport.http.ssl.SSLInvokerConstants;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.UnsupportedEncodingException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the actual concrete test for the invoker client.  Uses socket transport by default.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerClientTest extends TestCase implements SSLInvokerConstants, HandshakeCompletedListener
{
   private Client client;
   private static final Logger log = Logger.getLogger(InvokerClientTest.class);

   private String cipherSuite;
   private Certificate[] localCerts;
   private Certificate[] peerCerts;

   public void init()
   {
      try
      {
         Map config = new HashMap();
         config.put(Client.HANDSHAKE_COMPLETED_LISTENER, this);

         // since doing basic (using default ssl server socket factory)
         // need to set the system properties to the truststore
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + host + ":" + port);
         client = new Client(locator, "mock", config);
         client.connect();
      }
      catch (Exception e)
      {
         InvokerClientTest.log.error(e.getMessage(), e);
      }
   }

   public void testRemoteCall() throws Throwable
   {
      InvokerClientTest.log.debug("running testRemoteCall()");

      InvokerClientTest.log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      if ("bar".equals(ret))
      {
         InvokerClientTest.log.debug("PASS");
      }
      else
      {
         InvokerClientTest.log.debug("FAILED");
      }
      assertTrue("Result of testRemoteCall() invocation of foo.", "bar".equals(ret));

      assertTrue("CipherSuite = TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                 cipherSuite.equals("TLS_DHE_DSS_WITH_AES_128_CBC_SHA"));
//      X509Certificate localCert = (X509Certificate) localCerts[0];
//      assertTrue("LocalCert.SubjectDN = CN=unit-tests-client, OU=JBoss Inc., O=JBoss Inc., ST=Washington, C=US",
//                 localCert.getSubjectDN().getName().equals("CN=unit-tests-client, OU=JBoss Inc., O=JBoss Inc., ST=Washington, C=US"));


   }

   protected String getTransport()
   {
      return transport;
   }

   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

   public void setUp() throws Exception
   {
      init();
   }

   public void tearDown() throws Exception
   {
      if (client != null)
      {
         client.disconnect();
         client = null;
      }
   }

   public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent)
   {
      InvokerClientTest.log.info("handshakeCompleted, event=" + handshakeCompletedEvent);
      try
      {
         cipherSuite = handshakeCompletedEvent.getCipherSuite();
         InvokerClientTest.log.info("CipherSuite: " + cipherSuite);
         System.out.println("CipherSuite: " + cipherSuite);
         localCerts = handshakeCompletedEvent.getLocalCertificates();
         InvokerClientTest.log.info("LocalCertificates:");
         if (localCerts != null)
         {
            for (int n = 0; n < localCerts.length; n ++)
            {
               Certificate cert = localCerts[n];
               InvokerClientTest.log.info(cert);
               System.out.println(cert);
            }
         }
         InvokerClientTest.log.info("PeerCertificates:");
         System.out.println("PeerCertificates:");
         peerCerts = handshakeCompletedEvent.getPeerCertificates();
         if (peerCerts != null)
         {
            for (int n = 0; n < peerCerts.length; n ++)
            {
               Certificate cert = peerCerts[n];
               InvokerClientTest.log.info(cert);
               System.out.println(cert);
            }
         }
         SSLSession session = handshakeCompletedEvent.getSession();
         String[] names = session.getValueNames();
         for (int n = 0; n < names.length; n ++)
         {
            String name = names[n];
            InvokerClientTest.log.info(name + "=" + session.getValue(name));
            System.out.println(name + "=" + session.getValue(name));
         }
         String sessionID = null;
         byte[] id = handshakeCompletedEvent.getSession().getId();
         try
         {
            sessionID = new String(id, "UTF-8");
         }
         catch (UnsupportedEncodingException e)
         {
            InvokerClientTest.log.warn("Failed to create session id using UTF-8, using default", e);
            sessionID = new String(id);
         }
         System.out.println("sessionId: " + sessionID);
      }
      catch (SSLPeerUnverifiedException e)
      {
         InvokerClientTest.log.error("Failed to get peer cert", e);
      }
   }
}
