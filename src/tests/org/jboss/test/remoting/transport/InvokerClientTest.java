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

package org.jboss.test.remoting.transport;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.ComplexReturn;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.mock.MockInvokerCallbackHandler;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jboss.test.remoting.transport.mock.MockTest;

import java.net.BindException;
import java.rmi.server.UID;


/**
 * This is the actual concrete test for the invoker client.  Uses socket transport by default.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public abstract class InvokerClientTest extends TestCase
{
   private String sessionId = new UID().toString();
   private Client client;
   protected Connector connector;
   protected InvokerLocator locator;

   protected int port = 9091; //default port
   protected int callbackPort = -1;
   protected String metadata = null;

   public static final Logger log = Logger.getLogger(InvokerClientTest.class);

   public abstract String getTransport();

   public int getCallbackPort()
   {
      return callbackPort;
   }

   public void init()
   {
      try
      {
         String host = System.getProperty("jrunit.bind_addr", "localhost");
         String locatorURI = getTransport() + "://" + host+ ":" + port;
         if(metadata != null)
         {
            locatorURI = locatorURI + "/?" + metadata;
         }
         log.info("connecting to: " + locatorURI);
         InvokerLocator locator = new InvokerLocator(locatorURI);

         client = new Client(locator, "mock");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }

   /**
    * This will be used to create callback server
    *
    * @param port
    * @return
    * @throws Exception
    */
   protected InvokerLocator initServer(int port) throws Exception
   {
      if(port < 0)
      {
         port = TestUtil.getRandomPort();
      }
      log.debug("port = " + port);

//      InvokerRegistry.registerInvoker("mock", MockClientInvoker.class, MockServerInvoker.class);
      connector = new Connector();

      String locatorURI = getTransport() + "://localhost:" + port;
      if(metadata != null)
      {
         locatorURI = locatorURI + "/?" + metadata;
      }
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();

      return locator;
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }


   public void setUp() throws Exception
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);

      String newMetadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(newMetadata != null && newMetadata.length() > 0)
      {
         metadata = newMetadata;
         log.info("Using metadata: " + metadata);
      }

      newMetadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA_CALLBACK);
      if(newMetadata != null && newMetadata.length() > 0)
      {
         if(metadata == null)
         {
            metadata = newMetadata;
         }
         else
         {
            metadata += newMetadata;
         }

         log.info("Using metadata: " + metadata);
      }

      // this is a retry hack because in some cases, can get duplicate callback server ports
      // when trying to find a free one.
      int retryLimit = 3;
      for(int x = 0; x < retryLimit; x++)
      {
         try
         {
            locator = initServer(getCallbackPort());
         }
         catch(BindException e)
         {
            if(x + 1 == retryLimit)
            {
               throw e;
            }
            else
            {
               continue;
            }
         }
         break;
      }

      init();

   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
         connector = null;
      }
      locator = null;
      if(client != null)
      {
         client.disconnect();
         client = null;
      }
   }

   /**
    * Test simple invocation and adding of listener with push callback (meaning server
    * will send callback message when it gets it) to a local callback server
    *
    * @throws Throwable
    */
   public void testLocalPushCallback() throws Throwable
   {
      log.debug("running testLocalPushCallback()");

      sessionId = new UID().toString();

      sessionId = client.getSessionId();
      MockInvokerCallbackHandler handler = new MockInvokerCallbackHandler(sessionId);

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testLocalPushCallback() invocation of foo.", "bar".equals(ret));
      if("bar".equals(ret))
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testLocalPushCallback1");
      }

      client.addListener(handler, locator);
      // invoke which should cause callback
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(3000);
      log.debug("done sleeping.");
      int callbacksPerformed = handler.isCallbackReceived();
      log.debug("callbacksPerformed after adding listener is " + callbacksPerformed);
      assertTrue("Result of testLocalPushCallback() failed since did not get callback.",
                 (callbacksPerformed == 1));
      if(callbacksPerformed == 1)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testLocalPushCallback2");
      }
      // Can now call direct on client
      client.removeListener(handler);
      // shouldn't get callback now since removed listener
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(2000);
      log.debug("done sleeping.");
      callbacksPerformed = handler.isCallbackReceived();
      log.debug("callbackPerformed after removing listener is " + callbacksPerformed);
      assertTrue("Result of testLocalPushCallback() failed since did get callback " +
                 "but have been removed as listener.",
                 (callbacksPerformed == 1));
      if(callbacksPerformed == 1)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testLocalPushCallback3");
      }

   }

   /**
    * Test simple invocation and adding of listener with push callback (meaning server
    * will send callback message when it gets it) to a remote callback server
    *
    * @throws Throwable
    */
   public void testRemotePushCallback() throws Throwable
   {
      log.debug("running testRemotePushCallback()");

      sessionId = new UID().toString();
      //InvokerLocator locator = client.getInvoker().getLocator();
      sessionId = client.getSessionId();
      MockInvokerCallbackHandler handler = new MockInvokerCallbackHandler(sessionId);

      log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testRemotePushCallback() invocation of foo.", "bar".equals(ret));
      if("bar".equals(ret))
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testRemotePushCallback1");
      }

      client.addListener(handler, locator);
      // invoke which should cause callback
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(3000);
      log.debug("done sleeping.");
      // TODO: No way to currently check the remote callback handler
      // to see if it got callback -TME
      /*
      int callbacksPerformed = handler.isCallbackReceived();
      log.debug("callbacksPerformed after adding listener is " + callbacksPerformed);
      assertTrue("Result of testRemotePushCallback() failed since did not get callback.",
                 (callbacksPerformed == 1));
      */
      // Can now call direct on client
      client.removeListener(handler);
      // shouldn't get callback now since removed listener
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(2000);
      log.debug("done sleeping.");
      /*
      callbacksPerformed = handler.isCallbackReceived();
      log.debug("callbackPerformed after removing listener is " + callbacksPerformed);
      assertTrue("Result of testRemotePushCallback() failed since did get callback " +
                 "but have been removed as listener.",
                 (callbacksPerformed == 1));
      */
   }

   /**
    * Tests simple invocation and pull callbacks.  Meaning will add a listener and
    * will then have to get the callbacks from the server.
    *
    * @throws Throwable
    */
   public void testPullCallback() throws Throwable
   {
      log.debug("running testPullCallback()");

      // should be null by default, since don't have connector started, but setting anyway
      //client.setClientLocator(null);

      MockInvokerCallbackHandler handler = new MockInvokerCallbackHandler(sessionId);

      // simple invoke, should return bar
      Object ret = makeInvocation("bar", "foo");
      assertTrue("Result of runPullCallbackTest() invocation of bar.", "foo".equals(ret));
      if("foo".equals(ret))
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testPullCallback1");
      }

      client.addListener(handler);
      // invoke which should cause callback on server side
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(5000);
      ret = client.getCallbacks(handler);
      log.debug("getCallbacks returned " + ret);
      log.debug("should have something.");
      assertTrue("Result of runPullCallbackTest() getCallbacks() after add listener.",
                 ret != null);
      if(ret != null)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testPullCallback2");
      }

      // can now call directly on client
      //ret = makeInvocation("removeListener", null);
      client.removeListener(handler);
      ret = makeInvocation("getCallbacks", null);
      log.debug("getCallbacks returned " + ret);
      log.debug("should have been empty.");
      assertTrue("Result of runPullCallbackTest() getCallbacks() after remove listener.",
                 ret == null);
      if(ret == null)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testPullCallback3");
      }

   }

   /**
    * Tests complex invocation to get object containing array of complex objects.
    *
    * @throws Throwable
    */
   public void testArrayReturn() throws Throwable
   {
      // simple invoke, should return bar
      Object ret = makeInvocation("testComplexReturn", null);
      ComplexReturn complexRet = (ComplexReturn) ret;
      MockTest[] mockTests = complexRet.getMockTests();
      assertTrue("ComplexReturn's array should contain 2 items",
                 2 == mockTests.length);
      if(2 == mockTests.length)
      {
         log.debug("PASS");
      }
      else
      {
         log.debug("FAILED - testArrayReturn1");
      }

      for(int x = 0; x < mockTests.length; x++)
      {
         System.err.println(mockTests[x]);
         MockTest test = mockTests[x];
         assertNotNull("MockTest should not be null", test);
         if(test != null)
         {
            log.debug("PASS");
         }
         else
         {
            log.debug("FAILED - testArrayReturn2");
         }

      }

//            assertTrue("Result of runPullCallbackTest() invocation of bar.",
//                       "foo".equals(ret));
   }

   public void testThrownException() throws Throwable
   {
      try
      {
         makeInvocation("testException", null);
         assertTrue("Did not get exception thrown as expected.", false);
      }
      catch(Exception throwable)
      {
         assertTrue("Got exception thrown as expected.", true);
      }
   }

   protected Client getClient()
   {
      return client;
   }

   protected Object makeInvocation(String method, String param) throws Throwable
   {
      return client.invoke(new NameBasedInvocation(method,
                                                   new Object[]{param},
                                                   new String[]{String.class.getName()}),
                           null);
   }
}
