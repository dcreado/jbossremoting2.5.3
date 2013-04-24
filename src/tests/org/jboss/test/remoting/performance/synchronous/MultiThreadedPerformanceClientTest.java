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
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import junit.framework.Test;
import junit.framework.TestCase;
import org.jboss.jrunit.controller.ThreadLocalBenchmark;
import org.jboss.jrunit.decorators.ThreadLocalDecorator;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.performance.PerformanceReporter;

import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MultiThreadedPerformanceClientTest extends TestCase
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

   private static final Logger log = Logger.getLogger(MultiThreadedPerformanceClientTest.class);

   private SynchronizedInt callCounter = new SynchronizedInt(0);

   private int numOfThreads = 10;

   protected String host = "localhost";

   public static Test suite()
   {
      return new ThreadLocalDecorator(MultiThreadedPerformanceClientTest.class, 1);
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
      if(client != null)
      {
         log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());
      }

      PerformanceCallbackKeeper handler = addCallbackListener(sessionId, serverDoneLock);

      ThreadLocalBenchmark.openBench("Adding callback listener");

      // Need to add callback listener to get callback when the server is done
//      client.addListener(handler, locator, client.getSessionId());

      ThreadLocalBenchmark.closeBench("Adding callback listener");

      ThreadLocalBenchmark.openBench("Simple invocation");

      // simple invoke, should return bar
      Object ret = makeInvocation(NUM_OF_CALLS, new Integer(getNumberOfCalls()));

      ThreadLocalBenchmark.closeBench("Simple invocation");

      // create the payload object
      final byte[] payload = new byte[getPayloadSize()];
      // THIS IS WHERE TO START THE TIMING
      ThreadLocalBenchmark.openBench("Client calls");
      long startTime = System.currentTimeMillis();


      Thread[] threads = new Thread[numOfThreads];
      for(int x = 0; x < numOfThreads; x++)
      {
         Thread newThread = new Thread(new Runnable()
         {
            public void run()
            {
               try
               {
                  makeTestInvocation(payload);
               }
               catch(Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
         });
         threads[x] = newThread;
         newThread.start();
      }

      // will make timeout 2 seconds per call
      long timeoutPeriod = 2000 * getNumberOfCalls();

      for(int i = 0; i < threads.length; i++)
      {
         threads[i].join(timeoutPeriod);
      }

      long endCallTime = System.currentTimeMillis();
      System.out.println("Time to make all " + getNumberOfCalls() + " is " + (endCallTime - startTime));
      ThreadLocalBenchmark.closeBench("Client calls");

      //TODO: -TME Should make this configurable?
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

   public InvokerLocator getInvokerLocator()
   {
      return locator;
   }

   protected PerformanceCallbackKeeper addCallbackListener(String sessionId, Latch serverDoneLock)
         throws Throwable
   {
      org.jboss.test.remoting.performance.synchronous.PerformanceCallbackHandler handler = new org.jboss.test.remoting.performance.synchronous.PerformanceCallbackHandler(sessionId, serverDoneLock);
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

   private String getBenchmarkName()
   {
      return "MultiThreadedPerformanceClientTest";
   }

   private void makeTestInvocation(byte[] payload)
         throws Throwable
   {
      Payload payloadWrapper = new Payload(payload);

      while(callCounter.get() < getNumberOfCalls())
      {
         int currentVal = callCounter.increment();
         payloadWrapper.setCallNumber(currentVal);
         Object resp = makeClientCall(TEST_INVOCATION, payloadWrapper);
         assertEquals(currentVal, ((Integer) resp).intValue());
      }
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
      MultiThreadedPerformanceClientTest test = new MultiThreadedPerformanceClientTest();
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

   public static class PerformanceCallbackHandler implements InvokerCallbackHandler
   {
      private String sessionId;
      private Latch lock;
      private int numberOfCallsProcessed = 0;
      private int numberofDuplicates = 0;

      public PerformanceCallbackHandler(String sessionId, Latch lock)
      {
         this.sessionId = sessionId;
         this.lock = lock;
      }

      public int getNumberOfCallsProcessed()
      {
         return numberOfCallsProcessed;
      }

      public int getNumberOfDuplicates()
      {
         return numberofDuplicates;
      }

      /**
       * Will take the callback message and send back to client.
       * If client locator is null, will store them till client polls to get them.
       *
       * @param callback
       * @throws org.jboss.remoting.callback.HandleCallbackException
       *
       */
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         Object ret = callback.getCallbackObject();
         Integer[] handledArray = (Integer[]) ret;
         Integer numOfCallsHandled = (Integer) handledArray[0];
         Integer numOfDuplicates = (Integer) handledArray[1];
         System.out.println("Server is done.  Number of calls handled: " + numOfCallsHandled);
         numberOfCallsProcessed = numOfCallsHandled.intValue();
         System.out.println("Number of duplicate calls: " + numOfDuplicates);
         numberofDuplicates = numOfDuplicates.intValue();
         Object obj = callback.getCallbackHandleObject();
         String handbackObj = (String) obj;
         System.out.println("Handback object should be " + sessionId + " and server called back with " + handbackObj);
         lock.release();
      }
   }
}
