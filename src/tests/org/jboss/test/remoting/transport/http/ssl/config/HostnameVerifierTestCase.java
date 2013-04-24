/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.transport.http.ssl.config;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import javax.management.MBeanServer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p>
 * Copyright (c) May 20, 2006
 * </p>
 */
public class HostnameVerifierTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(HostnameVerifierTestCase.class);
   private static Connector connector;
   private static InvokerLocator locator;
   
  
   public void setUp()
   {
      try
      {
         if (connector == null)
         {
            // Register subclassed transport for test.
            AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  InvokerRegistry.registerInvokerFactories(getTransport(),
                        TestClientInvokerFactory.class,
                        TestServerInvokerFactory.class);
                  return null;
               }
            });
            
            HashMap config = new HashMap();
            
            // Put SSL keystore parameters in config map.
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
            String keyStoreFilePath = getClass().getResource("../.keystore").getFile();
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
            config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
            
            // Make callback Client use remote invoker.
            config.put(InvokerLocator.FORCE_REMOTE, "true");
            
            // Start Connector.
            int freeport = PortUtil.findFreePort(getHostName());
            locator = new InvokerLocator(getTransport() + "://" + getHostName() + ":" + freeport);
            connector = new Connector(locator, config);
            connector.create();
            connector.addInvocationHandler("sample", new SampleInvocationHandler());
            connector.start();
         }
      }
      catch (Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
   }
   
   
   public void shutDown()
   {
      try
      {
         
      }
      catch (Exception e)
      {
         log.error(e);
         e.printStackTrace();
      }
   }
   
   
   public void testSetClassNameMetadata()
   {
      log.info("entering " + getName());
      try
      {
         HashMap config = new HashMap();
         
         // Make Client use remote invoker.
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL truststore parameters in config map.
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath =  getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Put HostnameVerifier class name in metadata.
         HashMap metadata = new HashMap();
         metadata.put(HTTPSClientInvoker.HOSTNAME_VERIFIER, SelfIdentifyingHostnameVerifier.class.getName());
         
         // Connect Client and make invocation.
         Client client = new Client(locator, config);
         client.connect();
         client.invoke("abc", metadata);
         
         // Verify HostnameVerifier class is the class named in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(clientInvoker instanceof TestClientInvoker);
         TestClientInvoker testClientInvoker = (TestClientInvoker) clientInvoker;
         assertEquals(SelfIdentifyingHostnameVerifier.class.getName(),
                      testClientInvoker.getHostnameVerifierClassName());
         
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }
   
   
   public void testSetClassNameConfig()
   {
      log.info("entering " + getName());
      try
      {
         HashMap config = new HashMap();
         
         // Make Client use remote invoker.
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL truststore parameters in config map.
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath =  getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Put HostnameVerifier class name in config map.
         config.put(HTTPSClientInvoker.HOSTNAME_VERIFIER, SelfIdentifyingHostnameVerifier.class.getName());
         
         // Connect Client and make invocation.
         Client client = new Client(locator, config);
         client.connect();
         client.invoke("abc");
         
         // Verify HostnameVerifier class is the class named in config map.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(clientInvoker instanceof TestClientInvoker);
         TestClientInvoker testClientInvoker = (TestClientInvoker) clientInvoker;
         assertEquals(SelfIdentifyingHostnameVerifier.class.getName(),
                      testClientInvoker.getHostnameVerifierClassName());
         
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }
   
   
   public void testIgnoreDirectiveMetadata()
   {
      log.info("entering " + getName());
      try
      {
         HashMap config = new HashMap();
         
         // Make Client use remote invoker.
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL truststore parameters in config map.
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath =  getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Put "ignore host" directive in config map.
         HashMap metadata = new HashMap();
         metadata.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Connect Client and make invocation.
         Client client = new Client(locator, config);
         client.connect();
         client.invoke("abc", metadata);
         
         // Verify "ignore host" directive was honored.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(clientInvoker instanceof TestClientInvoker);
         TestClientInvoker testClientInvoker = (TestClientInvoker) clientInvoker;
         Class[] classes = HTTPSClientInvoker.class.getDeclaredClasses();
         String anyhostVerifyClassName = classes[0].getName();
         assertEquals(anyhostVerifyClassName, testClientInvoker.getHostnameVerifierClassName());
         
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }
   
   
   public void testIgnoreDirectiveConfig()
   {
      log.info("entering " + getName());
      try
      {
         HashMap config = new HashMap();
         
         // Make Client use remote invoker.
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         
         // Put SSL truststore parameters in config map.
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath =  getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
         
         // Put "ignore host" directive in config map.
         config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
         
         // Connect Client and make invocation.
         Client client = new Client(locator, config);
         client.connect();
         client.invoke("abc");
         
         // Verify "ignore host" directive was honored.
         ClientInvoker clientInvoker = client.getInvoker();
         assertTrue(clientInvoker instanceof TestClientInvoker);
         TestClientInvoker testClientInvoker = (TestClientInvoker) clientInvoker;
         Class[] classes = HTTPSClientInvoker.class.getDeclaredClasses();
         String anyhostVerifyClassName = classes[0].getName();
         assertEquals(anyhostVerifyClassName, testClientInvoker.getHostnameVerifierClassName());
         
         log.info(getName() + " PASSES");
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         log.info(getName() + " FAILS");
         fail();
      }
   }
   
   
   public void testZZZ()
   {
   }
   
   protected String getHostName()
   {
      return "localhost";
   }
   
   protected String getTransport()
   {
      return "https";
   }
   
   
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler callbackHandler;
      
      public SampleInvocationHandler()
      {   
      }
      
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return new Integer(0);
      }
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         log.info("entering addListener()");
         this.callbackHandler = callbackHandler;
         
         try
         {
            Callback callback = new Callback(new Integer(1));
            callbackHandler.handleCallback(callback);
            log.info("sent first callback");
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
      
      public InvokerCallbackHandler getCallbackHandler()
      {
         return callbackHandler;
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
