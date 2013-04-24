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

package org.jboss.test.remoting.performance.synchronous;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.Test;
import junit.framework.TestCase;
import org.jboss.jrunit.controller.ThreadLocalBenchmark;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.PerformanceReporter;

import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class PerformanceClientTest extends TestCase
{
   private Client client;
   private Connector connector;
   private InvokerLocator locator;

   private Latch serverDoneLock = new Latch();

   // default transport and port
   private String transport = "socket";
   private String serialization = "";
   private String metadata = null;
   private int port = 9090;

   // performance specific variables
   private int numberOfCalls = 500;
   private int sizeOfPayload = 1024;

   private String sessionId = null;

   // statics for the specific call methods
   public static final String NUM_OF_CALLS = "numOfCalls";
   public static final String TEST_INVOCATION = "testInvocation";

   protected static final Logger log = Logger.getLogger(PerformanceClientTest.class);

   protected String host = "localhost";

   public static Test suite()
   {
      return new ThreadLocalDecorator(PerformanceClientTest.class, 1);
   }

   public InvokerLocator getInvokerLocator()
   {
      return locator;
   }

   public void init()
   {
      try
      {
         String locatorURI = getTransport() + "://" + host + ":" + getPort();
         if(metadata != null)
         {
            locatorURI = locatorURI + "/?" + metadata;
         }
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("starting remoting client with locator of " + locator);
         log.debug("starting remoting client with locator of " + locator);
         client = new Client(locator, "performance");
         client.connect();
         log.info("Client connected to " + locator.getLocatorURI());
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }

      sessionId = client.getSessionId();
   }

   public String getTransport()
   {
      return transport;
   }

   public String getSerialization()
   {
      return serialization;
   }

   public int getPort()
   {
      return port;
   }

   protected String getBenchmarkName()
   {
      return "PerformanceClientTest";
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

      connector = new Connector();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      if(metadata != null)
      {
         locatorURI = locatorURI + "/?" + metadata;
      }
      InvokerLocator locator = new InvokerLocator(locatorURI);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.start();
      log.info("Callback server started on " + locator.getLocatorURI());
      return locator;
   }


   public void setUp() throws Exception
   {
      String newTransport = System.getProperty(PerformanceTestCase.REMOTING_TRANSPORT);
      if(newTransport != null && newTransport.length() > 0)
      {
         transport = newTransport;
         log.info("Using transport: " + transport);
      }

      String newHost = System.getProperty(PerformanceTestCase.REMOTING_HOST);
      if(newHost != null && newHost.length() > 0)
      {
         host = newHost;
         log.info("Using host: " + host);
      }

      String newSerialization = System.getProperty(PerformanceTestCase.REMOTING_SERIALIZATION);
      if(newSerialization != null && newSerialization.length() > 0)
      {
         serialization = newSerialization;
         log.info("Using serialization: " + serialization);
      }

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

      String payloadSize = System.getProperty(PerformanceTestCase.PAYLOAD_SIZE);
      if(payloadSize != null && payloadSize.length() > 0)
      {
         try
         {
            sizeOfPayload = Integer.parseInt(payloadSize);
            log.info("Using payload size: " + sizeOfPayload);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      }

      String numOfCallsParam = System.getProperty(PerformanceTestCase.NUMBER_OF_CALLS);
      if(numOfCallsParam != null && numOfCallsParam.length() > 0)
      {
         try
         {
            numberOfCalls = Integer.parseInt(numOfCallsParam);
            log.info("Using number of calls: " + numberOfCalls);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      }

      locator = initServer(-1);
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

   protected int getNumberOfCalls()
   {
      return numberOfCalls;
   }

   protected int getPayloadSize()
   {
      return sizeOfPayload;
   }

   public void testClientCalls() throws Throwable
   {
      Map metadata = new HashMap();
      populateMetadata(metadata);

      ThreadLocalBenchmark.openBench(getBenchmarkName(), metadata);

      log.debug("running testSynchronousCalls()");
      log.info("This class is " + getBenchmarkName());
      if(client != null)
      {
         log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());
      }

      ThreadLocalBenchmark.openBench("Adding callback listener");

      PerformanceCallbackKeeper handler = addCallbackListener(sessionId, serverDoneLock);

      ThreadLocalBenchmark.closeBench("Adding callback listener");

      ThreadLocalBenchmark.openBench("Simple invocation");

      // simple invoke, should return bar
      Object ret = makeInvocation(NUM_OF_CALLS, new Integer(getNumberOfCalls()));

      ThreadLocalBenchmark.closeBench("Simple invocation");

//      // create the payload object
//      byte[] payload = new byte[getPayloadSize()];
//      Payload payloadWrapper = new Payload(payload);

      // THIS IS WHERE TO START THE TIMING
      ThreadLocalBenchmark.openBench("Client calls");
      long startTime = System.currentTimeMillis();

      for(int x = 0; x < getNumberOfCalls(); x++)
      {
         // create the payload object
         byte[] payload = new byte[getPayloadSize()];
         Payload payloadWrapper = new Payload(payload);
//         payloadWrapper.setCallNumber(callCounter.increment());
         payloadWrapper.setCallNumber(x);
         Object resp = makeClientCall(TEST_INVOCATION, payloadWrapper);
         verifyResults(x, resp);
      }

      long endCallTime = System.currentTimeMillis();
      System.out.println("Time to make all " + getNumberOfCalls() + " is " + (endCallTime - startTime));
      ThreadLocalBenchmark.closeBench("Client calls");

      //TODO: -TME Should make this configurable?
      // will make timeout 2 seconds per call
      long timeoutPeriod = 2000 * getNumberOfCalls();
      boolean didComplete = serverDoneLock.attempt(timeoutPeriod);
      long endProcessTime = System.currentTimeMillis();
      if(didComplete)
      {
         int numProcessed = handler.getNumberOfCallsProcessed();
         int numOfDuplicates = handler.getNumberOfDuplicates();
         long totalTime = (endProcessTime - startTime);
         System.out.println("Time for server to process " + numProcessed + " is " + totalTime);
         if(getNumberOfCalls() == numProcessed)
         {
            System.out.println("PASSED - the server processed all calls.");
         }
         else
         {
            System.out.println("FAILED - the server did NOT process all calls.");
         }
         assertEquals("The total number of calls should equal total number processed.", getNumberOfCalls(), numProcessed);
         assertEquals("The number of duplicates should be 0.", 0, numOfDuplicates);

         //TODO: -TME - This needs to be replaced by benchmark code reporting
//         metadata.put("server total count", String.valueOf(numProcessed));
//
//
//         PerformanceReporter.writeReport(this.getClass().getName(),
//                                         totalTime, getNumberOfCalls(), metadata);

      }
      else
      {
         System.out.println("FAILED - timed out waiting for server to call back when done processing.  Waited for " + timeoutPeriod);
         PerformanceReporter.writeReport("Error in test.  Server never replied that it finished processing.", 0, 0, null);
      }

      ThreadLocalBenchmark.closeBench(getBenchmarkName());


   }

   protected PerformanceCallbackKeeper addCallbackListener(String sessionId, Latch serverDoneLock)
         throws Throwable
   {
      PerformanceCallbackHandler handler = new PerformanceCallbackHandler(sessionId, serverDoneLock);
      // Need to add callback listener to get callback when the server is done
      client.addListener(handler, getInvokerLocator(), sessionId);
      return handler;
   }

   protected void populateMetadata(Map metadata)
   {
      metadata.put("alias", getBenchmarkAlias());
      metadata.put("transport", getTransport());
      metadata.put("number of client calls", String.valueOf(getNumberOfCalls()));
      metadata.put("payload size", String.valueOf(getPayloadSize()));
      metadata.put("serialization", serialization);
   }

   protected Object getBenchmarkAlias()
   {
      String config = System.getProperty("alias");
      if(config == null || config.length() == 0)
      {
         config = System.getProperty("jboss-junit-configuration");
         if(config == null || config.length() == 0)
         {
            config = getTransport() + "_" + getNumberOfCalls() + "_" + getPayloadSize() + "_" + serialization;
         }
      }
      return config;
   }

   protected void verifyResults(int x, Object resp)
   {
      assertEquals("The call number should be same as the numbe returned.", x, ((Integer) resp).intValue());
   }

   protected Object makeClientCall(String testInvocation, Payload payloadWrapper) throws Throwable
   {
      return makeInvocation(TEST_INVOCATION, payloadWrapper);
   }

   protected Object makeInvocation(String method, Object param) throws Throwable
   {
      Object ret = null;

      if(param != null)
      {
         ret = client.invoke(new NameBasedInvocation(method,
                                                     new Object[]{param},
                                                     new String[]{param.getClass().getName()}),
                             null);
      }
      else
      {
         throw new Exception("To make invocation, must pass a valid, non null, Object as parameter.");
      }

      return ret;
   }

   protected Object makeOnewayInvocation(String method, Object param, boolean isClientSide) throws Throwable
   {
      if(param != null)
      {
         client.invokeOneway(new NameBasedInvocation(method,
                                                     new Object[]{param},
                                                     new String[]{param.getClass().getName()}),
                             null,
                             isClientSide);
      }
      else
      {
         throw new Exception("To make oneway invocation, must pass a valid, non null, Object as parameter.");
      }
      return null;
   }


   public static void main(String[] args)
   {
      PerformanceClientTest test = new PerformanceClientTest();
      try
      {
         test.setUp();
         test.testClientCalls();
         test.tearDown();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }

}
