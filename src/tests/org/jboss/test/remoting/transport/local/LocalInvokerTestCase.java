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

package org.jboss.test.remoting.transport.local;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.rmi.server.UID;

/**
 * Test for the Local client invoker to verify will call directly on
 * local server invoker.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class LocalInvokerTestCase extends TestCase
{
   private String sessionId = new UID().toString();
   private Client client;
   private String transport = "socket";
   private int port = -1;
   private InvokerLocator locator;
   private static final String NAME = "LocalInvokerTest.class";
   private static final Logger log = Logger.getLogger(LocalInvokerTestCase.class);

   public LocalInvokerTestCase()
   {
      super(NAME);
   }

   public LocalInvokerTestCase(String name)
   {
      super(name);
   }

   public LocalInvokerTestCase(String transport, int port)
   {
      super(NAME);
      this.transport = transport;
      this.port = port;
   }

   public void init()
   {
      try
      {
         client = new Client(locator, "mock");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }

   /**
    * Create and initalize local server
    *
    * @param port
    * @return
    * @throws Exception
    */
   private InvokerLocator initServer(int port) throws Exception
   {
      if(port < 0)
      {
         port = TestUtil.getRandomPort();
      }
      log.debug("port = " + port);

//      InvokerRegistry.registerInvoker("mock", MockClientInvoker.class, MockServerInvoker.class);
      Connector connector = new Connector();
      InvokerLocator locator = new InvokerLocator(transport + "://localhost:" + port);
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
      this.port = port;
      this.locator = connector.getLocator();
      return locator;
   }


   public void testLocalInvoke() throws Throwable
   {
      try
      {
         log.debug("running testLocalInvoke()");

         InvokerLocator locator = initServer(-1);
         init();

//            MockInvokerCallbackHandler handler = new MockInvokerCallbackHandler(sessionId);

         log.debug("client.getInvoker()" + client.getInvoker());

         // simple invoke, should return bar
         Object ret = makeInvocation("foo", "bar");
         assertTrue("Result of testLocalInvoke() invocation of foo.", "bar".equals(ret));
         /*
         client.addListener(handler, locator);
         // invoke which should cause callback
         ret = makeInvocation("test", "test");
         // allow time for callback
         Thread.sleep(3000);
         log.debug("done sleeping.");
         boolean callbackPerformed = handler.isCallbackReceived();
         log.debug("callbackPerformed after adding listener is " + callbackPerformed);
         assertTrue("Result of runPushCallbackTest() failed since did not get callback.",
                    callbackPerformed);
         handler.isCallbackReceived(false);
         // Can now call direct on client
         //ret = makeInvocation("removeListener", null);
         client.removeListener(handler);
         // shouldn't get callback now since removed listener
         ret = makeInvocation("test", "test");
         // allow time for callback
         Thread.sleep(2000);
         log.debug("done sleeping.");
         callbackPerformed = handler.isCallbackReceived();
         log.debug("callbackPerformed after removing listener is " + callbackPerformed);
         assertTrue("Result of runPushCallbackTest() failed since did get callback " +
                    "but have been removed as listener.",
                    !callbackPerformed);
     */
      }
      finally
      {
         if(client != null)
         {
            client.disconnect();
         }
      }
   }

   /**
    * Tests simple invocation and pull callbacks.  Meaning will add a listener and
    * will then have to get the callbacks from the server.
    * @throws Throwable
    */
   /*
   public void testPullCallback() throws Throwable
   {
       try
       {
           log.debug("running testPullCallback()");

           init();
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
           ret = client.getCallbacks();
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
       finally
       {
           if(client != null)
           {
               client.disconnect();
               client = null;
           }
       }
   }
   */

   /**
    * Tests complex invocation to get object containing array of complex objects.
    *
    * @throws Throwable
    */
   /*
   public void testArrayReturn() throws Throwable
   {
       try
       {
           init();

           // simple invoke, should return bar
           Object ret = makeInvocation("testComplexReturn", null);
           ComplexReturn complexRet = (ComplexReturn)ret;
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
       finally
       {
           if(client != null)
           {
               client.disconnect();
               client = null;
           }
       }
   }
   */
   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

   public static Test suite()
   {
      return new TestSuite(LocalInvokerTestCase.class);
   }


   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);

      LocalInvokerTestCase client = null;
      if(args.length == 2)
      {
         String transport = args[0];
         int port = Integer.parseInt(args[1]);
         client = new LocalInvokerTestCase(transport, port);
      }
      else
      {
         client = new LocalInvokerTestCase(LocalInvokerTestCase.class.getName());
         System.out.println("Using default transport " +
                            "and default port." +
                            "\nCan enter transport and port.");
      }

      try
      {
         //regular class run
         //client.testLocalInvoke();
         //client.runInvokers();
//            MultipleTestRunner runner = new MultipleTestRunner();
//            runner.doRun(client, true);
         junit.textui.TestRunner.run(suite());

      }
      catch(Throwable e)
      {
         e.printStackTrace();
         System.exit(1);
      }
      System.exit(0);
   }
}
