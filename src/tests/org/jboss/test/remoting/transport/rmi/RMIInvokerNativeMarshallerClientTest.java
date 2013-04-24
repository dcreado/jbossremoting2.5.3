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

package org.jboss.test.remoting.transport.rmi;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.rmi.RMIServerInvoker;
import org.jboss.test.remoting.ComplexReturn;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;
import org.jboss.test.remoting.transport.mock.MockInvokerCallbackHandler;
import org.jboss.test.remoting.transport.mock.MockTest;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.rmi.MarshalledObject;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the actual concrete test for the invoker client.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class RMIInvokerNativeMarshallerClientTest extends TestCase
{

   private String sessionId = new UID().toString();
   private Client client;
   private Connector connector;
   private InvokerLocator locator;
   private int port = RMIServerInvoker.DEFAULT_REGISTRY_PORT - 1;
   protected String transport = "rmi";

   private static final Logger log = Logger.getLogger(RMIInvokerNativeMarshallerClientTest.class);

   public void init(Map metadata)
   {
      try
      {
         InvokerLocator locator = new InvokerLocator(buildLocatorURI(metadata, this.port));
         //InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + port);
         System.out.println("client locator: " + locator);
         client = new Client(locator, "mock");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }

   private String buildLocatorURI(Map metadata, int port)
   {
      if(metadata == null || metadata.size() == 0)
      {
         return transport + "://localhost:" + port;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(transport + "://localhost:" + port);

         Set keys = metadata.keySet();
         if(keys.size() > 0)
         {
            uriBuffer.append("/?");
         }

         Iterator itr = keys.iterator();
         while(itr.hasNext())
         {
            String key = (String) itr.next();
            String value = (String) metadata.get(key);
            uriBuffer.append(key + "=" + value + "&");
         }
         return uriBuffer.substring(0, uriBuffer.length() - 1);
      }
   }

   private InvokerLocator initServer(Map metadata, int serverPort) throws Exception
   {
      if(serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }
      log.debug("port = " + serverPort);

//      InvokerRegistry.registerInvoker("mock", MockClientInvoker.class, MockServerInvoker.class);
      connector = new Connector();

      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metadata, serverPort));
      System.out.println("Callback locator: " + locator);

      //InvokerLocator locator = new InvokerLocator(transport + "://localhost:" + port);
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<handlers>\n");
      buf.append("  <handler subsystem=\"mock\">org.jboss.test.remoting.transport.mock.MockServerInvocationHandler</handler>\n");
      buf.append("</handlers>\n");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.setConfiguration(xml.getDocumentElement());
      //connector.create();
      connector.start();
      return locator;
   }


   public void setUp() throws Exception
   {
      Map metadata = new HashMap();
      String newMetadata = System.getProperty(PerformanceTestCase.REMOTING_METADATA);
      if(newMetadata != null && newMetadata.length() > 0)
      {
         metadata.putAll(PerformanceServerTest.parseMetadataString(newMetadata));
      }

      metadata.put(RMIServerInvoker.REGISTRY_PORT_KEY, String.valueOf(port + 1));
      addMetadata(metadata);
      locator = initServer(metadata, -1);
      init(metadata);
      log.info("Using metadata: " + metadata);
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
      client.addListener(handler);
      // invoke which should cause callback on server side
      ret = makeInvocation("test", "test");
      // allow time for callback
      Thread.sleep(2000);
      ret = client.getCallbacks(handler);
      log.debug("getCallbacks returned " + ret);
      log.debug("should have something.");
      assertTrue("Result of runPullCallbackTest() getCallbacks() after add listener.",
                 ret != null);
      // can now call directly on client
      //ret = makeInvocation("removeListener", null);
      client.removeListener(handler);
      ret = makeInvocation("getCallbacks", null);
      log.debug("getCallbacks returned " + ret);
      log.debug("should have been empty.");
      assertTrue("Result of runPullCallbackTest() getCallbacks() after remove listener.",
                 ret == null);
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
      for(int x = 0; x < mockTests.length; x++)
      {
         System.err.println(mockTests[x]);
         MockTest test = mockTests[x];
         assertNotNull("MockTest should not be null", test);
      }

//            assertTrue("Result of runPullCallbackTest() invocation of bar.",
//                       "foo".equals(ret));
   }

   /**
    * Tests complex invocation to get marshalled object.
    *
    * @throws Throwable
    */
   public void testMarshalledObjectReturn() throws Throwable
   {
      // simple invoke, should return bar
      Object ret = makeInvocation("testMarshalledObject", null);
      ret = ((MarshalledObject) ret).get();
      ComplexReturn complexRet = (ComplexReturn) ret;
      MockTest[] mockTests = complexRet.getMockTests();
      assertTrue("ComplexReturn's array should contain 2 items",
                 2 == mockTests.length);
      for(int x = 0; x < mockTests.length; x++)
      {
         System.err.println(mockTests[x]);
         MockTest test = mockTests[x];
         assertNotNull("MockTest should not be null", test);
      }

//            assertTrue("Result of runPullCallbackTest() invocation of bar.",
//                       "foo".equals(ret));
   }
   
   protected void addMetadata(Map metadata)
   {
   }

   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

//   public static void main(String[] args)
//   {
//      RMIInvokerClientTest client = new RMIInvokerClientTest();
//      try
//      {
//         client.setUp();
//         client.testArrayReturn();
//         client.testArrayReturnWithDataType();
//         client.testLocalPushCallback();
//         client.testLocalPushCallbackWithDatatype();
//         client.testMarshalledObjectReturn();
//         client.testMarshalledObjectReturnWithDataType();
//         client.testPullCallback();
//         client.testPullCallbackWithDataType();
//         client.testRemotePushCallback();
//         client.testRemotePushCallbackWithDataType();
//      }
//      catch (Throwable throwable)
//      {
//         throwable.printStackTrace();
//      }
//      finally
//      {
//         try
//         {
//            client.tearDown();
//         }
//         catch (Exception e)
//         {
//            e.printStackTrace();
//         }
//      }
//
//   }

}
