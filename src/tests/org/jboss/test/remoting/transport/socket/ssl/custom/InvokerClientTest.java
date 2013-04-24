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

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.security.CustomSSLSocketFactory;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:tom@jboss.com">Tom Elrod</a>
 * @version $Revision: 2901 $
 * <p>
 * Copyright (c) Mar 23, 2006
 * </p>
 */
public class InvokerClientTest extends TestCase implements SSLInvokerConstants
{
   private static final Logger log = Logger.getLogger(InvokerClientTest.class);
   
   protected Client client;
   
   public void init()
   {
      try
      {
         Map config = new HashMap();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + port);
         client = new Client(locator, config);
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }
   
   public void testRemoteCall() throws Throwable
   {
      log.debug("running testRemoteCall()");
      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());
      
      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testRemoteCall() invocation of foo.", "bar".equals(ret));
      if("bar".equals(ret))
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED");
      }
      assertEquals("bar", ret);
      
   }
   
   public void testSocketFactoryParameters()
   {
      try
      {
         Map config = new HashMap();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
         String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
         config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
         
         InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + port);
         client = new Client(locator, config);
         client.connect();
         
         // Test that parameters are properly set.
         RemoteClientInvoker clientInvoker = (RemoteClientInvoker) client.getInvoker();
         CustomSSLSocketFactory socketFactory = (CustomSSLSocketFactory) clientInvoker.getSocketFactory();
         assertTrue(socketFactory.getSSLSocketBuilder().getTrustStoreType().equals("JKS"));
         //         assertTrue(socketFactory.gett.equals("JKS"));
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
      
   }
   
   public void testCallbacks()
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + callbackPort);
         Connector callbackConnector = new Connector(locator.getLocatorURI());
         callbackConnector.setServerSocketFactory(createServerSocketFactory());
         callbackConnector.create();
         callbackConnector.addInvocationHandler("sample", new SampleInvocationHandler());
         callbackConnector.start();

         CallbackHandler callbackHandler = new CallbackHandler();
         String callbackHandleObject = "myCallbackHandleObject";
         client.addListener(callbackHandler, locator, callbackHandleObject);
         solicitCallback("abc");
         
         // need to wait for brief moment so server can callback
         Thread.sleep(2000);
         
         // remove callback handler from server
         client.removeListener(callbackHandler);
         
         // shut down callback server
         callbackConnector.stop();
         callbackConnector.destroy();
         callbackConnector = null;
         
         List callbacks = callbackHandler.getCallbacks();
         assertEquals(callbacks.size(), 1);
//         assertEquals(callbacks.get(0), "abc");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         fail();
      }
   }
   
   protected String getTransport()
   {
      return transport;
   }
   
   protected Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
            new Object[]{param},
            new String[]{String.class.getName()}),
            null);
      
      return ret;
   }
   
   protected Object solicitCallback(String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation("test",
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
      if(client != null)
      {
         client.disconnect();
         client = null;
      }
   }
   
   
   protected ServerSocketFactory createServerSocketFactory()
   throws NoSuchAlgorithmException, KeyManagementException, IOException,
   CertificateException, UnrecoverableKeyException, KeyStoreException
   {
      ServerSocketFactory serverSocketFactory = null;
      
      SSLSocketBuilder server = new SSLSocketBuilder();
      server.setUseSSLServerSocketFactory(false);
      
      server.setSecureSocketProtocol("SSL");
      server.setTrustStoreAlgorithm("SunX509");
      
      server.setTrustStoreType("JKS");
      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
      server.setTrustStoreURL(trustStoreFilePath);
      server.setTrustStorePassword("unit-tests-client");
//      server.setUseClientMode(true);
      server.setServerSocketUseClientMode(true);
      server.setSocketUseClientMode(false);
      /*
       * This is optional since if not set, will use
       * the key store password (and are the same in this case)
       */
      //server.setKeyPassword("unit-tests-server");
      
      serverSocketFactory = server.createSSLServerSocketFactory();
      
      return serverSocketFactory;
   }
   
   
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public SampleInvocationHandler()
      {   
      }
      
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return new Integer(0);
      }
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("entering addListener()");
         
         try
         {
            Callback callback = new Callback(new Integer(1));
            callbackHandler.handleCallback(callback);
            System.out.println("sent first callback");
            callback = new Callback(new Integer(2));
            callbackHandler.handleCallback(callback);
            System.out.println("sent second callback");
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }
      
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }
      
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }
   }
   
   
   public static class CallbackHandler implements InvokerCallbackHandler
   {
      private ArrayList callbacks = new ArrayList();
      
      public CallbackHandler()
      {
      }
      
      public ArrayList getCallbacks()
      {
         return callbacks;
      }
      
      /**
       * Will take the callback and print out its values.
       *
       * @param callback
       * @throws org.jboss.remoting.callback.HandleCallbackException
       *
       */
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("Received push callback.");
         log.info("Received callback value of: " + callback.getCallbackObject());
         log.info("Received callback server invoker of: " + callback.getServerLocator());
         callbacks.add(callback);
      }
   }
}
